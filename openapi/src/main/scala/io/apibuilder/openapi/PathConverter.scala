package io.apibuilder.openapi

import io.apibuilder.spec.v0.{models => ab}
import io.apibuilder.validation.ScalarType
import sttp.apispec.{ExampleSingleValue, Schema, SchemaLike}
import sttp.apispec.openapi._

import scala.collection.immutable.ListMap

case class PathConversionResult(
  resources: Seq[ab.Resource],
  pathReports: Seq[PathReport],
)

class PathConverter(
  modelReferences: Map[String, String],
  config: NamingConfig,
  filterHeaders: Set[String] = Set.empty,
  requestBodies: ListMap[String, Either[Reference, RequestBody]] = ListMap.empty,
) {

  private val filterHeadersLower: Set[String] = filterHeaders.map(_.toLowerCase)

  import NamingUtils._
  import SchemaResolver._

  private val HttpMethods: Seq[(String, PathItem => Option[Operation])] = Seq(
    "GET" -> (_.get),
    "PUT" -> (_.put),
    "POST" -> (_.post),
    "DELETE" -> (_.delete),
    "PATCH" -> (_.patch),
    "HEAD" -> (_.head),
    "OPTIONS" -> (_.options),
    "TRACE" -> (_.trace),
  )

  def convertPaths(paths: Paths): PathConversionResult = {
    val allResources = Seq.newBuilder[ab.Resource]
    val pathReports = Seq.newBuilder[PathReport]

    paths.pathItems.toSeq.foreach { case (path, pathItem) =>
      val (resourceOpt, report) = convertPathItem(path, pathItem)
      resourceOpt.foreach(allResources += _)
      pathReports += report
    }

    val merged = allResources
      .result()
      .filterNot(r => NamingUtils.ApibuilderPrimitiveTypes.contains(r.`type`))
      .groupBy(_.`type`)
      .toSeq
      .sortBy(_._1)
      .map { case (_, group) =>
        val first = group.head
        first.copy(operations = group.flatMap(_.operations))
      }

    PathConversionResult(
      resources = merged,
      pathReports = pathReports.result(),
    )
  }

  private def convertPathItem(path: String, pathItem: PathItem): (Option[ab.Resource], PathReport) = {
    val issues = Seq.newBuilder[String]
    val methods = HttpMethods.flatMap { case (method, extract) =>
      extract(pathItem).map(method -> _)
    }

    pathItem.parameters.foreach {
      case Left(ref) =>
        issues += s"$path: parameter reference '${ref.$ref}' (not resolved)"
      case Right(p) if p.in == ParameterIn.Cookie =>
        issues += s"$path: cookie parameter '${p.name}' (not supported)"
      case _ => ()
    }

    val pathLevelParams = extractParams(pathItem.parameters)

    val operations = methods.flatMap { case (method, op) =>
      op.parameters.foreach {
        case Left(ref) =>
          issues += s"$method $path: parameter reference '${ref.$ref}' (not resolved)"
        case Right(p) if p.in == ParameterIn.Cookie =>
          issues += s"$method $path: cookie parameter '${p.name}' (not supported)"
        case _ => ()
      }

      op.requestBody.foreach {
        case Right(rb) if !rb.content.contains("application/json") =>
          val nonJsonTypes = rb.content.keys.toSeq
          if (nonJsonTypes.nonEmpty)
            issues += s"$method $path: no JSON request body (${nonJsonTypes.mkString(", ")})"
        case _ => ()
      }

      op.responses.responses.foreach { case (key, respOrRef) =>
        respOrRef.foreach { resp =>
          if (!resp.content.contains("application/json")) {
            val nonJsonTypes = resp.content.keys.toSeq
            if (nonJsonTypes.nonEmpty)
              issues += s"$method $path response ${formatResponseKey(key)}: no JSON content (${nonJsonTypes.mkString(", ")})"
          }
        }
      }

      if (op.security.nonEmpty) issues += s"$method $path: security requirements (not converted)"
      if (op.callbacks.nonEmpty) issues += s"$method $path: callbacks (not converted)"

      val opParams = extractParams(op.parameters)
      val merged = mergeParameters(pathLevelParams, opParams)
      Some(convertOperation(path, method, op, merged))
    }

    val nonUnitResponses = operations.view
      .flatMap(_.responses)
      .filter(_.`type` != ScalarType.UnitType.name)

    val primaryResponse = nonUnitResponses
      .collectFirst { case r if is2xx(r.code) => r }
      .orElse(nonUnitResponses.headOption)

    val resource = primaryResponse.map { firstResponse =>
      ab.Resource(
        `type` = firstResponse.`type`,
        plural = firstResponse.`type` + "s",
        path = Some(""),
        description = None,
        deprecation = None,
        operations = operations,
        attributes = Seq.empty,
      )
    }

    if (operations.nonEmpty && resource.isEmpty)
      issues += s"$path: no typed response found; path excluded from resources"

    val report = PathReport(
      path = path,
      methods = methods.map(_._1),
      unsupported = issues.result(),
    )

    (resource, report)
  }

  private def convertOperation(
    path: String,
    httpMethod: String,
    op: Operation,
    mergedParams: Seq[Parameter],
  ): ab.Operation =
    ab.Operation(
      method = ab.Method.fromString(httpMethod).getOrElse(ab.Method.UNDEFINED(httpMethod)),
      path = toApibuilderPath(path),
      description = op.description,
      deprecation = None,
      body = op.requestBody.flatMap(extractBody),
      parameters = mergedParams.flatMap(convertParameter),
      responses = op.responses.responses.toSeq.map(convertResponse),
      attributes = Seq.empty,
    )

  private def convertResponse(entry: (ResponsesKey, Either[Reference, Response])): ab.Response = {
    val (key, responseOrRef) = entry
    val code: ab.ResponseCode = key match {
      case ResponsesCodeKey(c) => ab.ResponseCodeInt(c)
      case ResponsesDefaultKey => ab.ResponseCodeOption.Default
      case ResponsesRangeKey(r) => ab.ResponseCodeInt(r * 100)
    }
    val (desc, typeName) = responseOrRef match {
      case Right(r) =>
        (
          Option(r.description).filter(_.nonEmpty),
          jsonSchemaRef(r.content).map(resolve).getOrElse(ScalarType.UnitType.name),
        )
      case Left(ref) =>
        val t =
          if (ref.$ref.startsWith("#/components/schemas/")) resolve(refName(ref.$ref))
          else ScalarType.UnitType.name
        (None, t)
    }
    ab.Response(
      code = code,
      `type` = sn(typeName),
      headers = None,
      description = desc,
      deprecation = None,
      attributes = None,
    )
  }

  private def extractBody(bodyOrRef: Either[Reference, RequestBody]): Option[ab.Body] =
    bodyOrRef match {
      case Right(rb) =>
        jsonSchemaRef(rb.content).map(t => ab.Body(`type` = sn(resolve(t))))
      case Left(ref) =>
        resolveRequestBodyRef(ref.$ref).map(t => ab.Body(`type` = sn(resolve(t))))
    }

  private def resolveRequestBodyRef(ref: String, seen: Set[String] = Set.empty): Option[String] = {
    if (seen.contains(ref)) return None
    val prefix = "#/components/requestBodies/"
    if (ref.startsWith(prefix)) {
      val name = ref.stripPrefix(prefix)
      requestBodies.get(name).flatMap {
        case Right(rb) => jsonSchemaRef(rb.content)
        case Left(nestedRef) => resolveRequestBodyRef(nestedRef.$ref, seen + ref)
      }
    } else if (ref.startsWith("#/components/schemas/")) {
      Some(refName(ref))
    } else {
      System.err.println(s"Warning: cannot resolve requestBody reference '$ref'; operation will have no body")
      None
    }
  }

  private def jsonSchemaRef(content: ListMap[String, MediaType]): Option[String] =
    content.get("application/json").flatMap(_.schema.flatMap(schemaRef))

  private def schemaRef(sl: SchemaLike): Option[String] = sl match {
    case s: Schema if s.$ref.isDefined => Some(refName(s.$ref.get))
    case s: Schema if s.oneOf.nonEmpty =>
      s.oneOf.collectFirst { case r: Schema if r.$ref.isDefined => refName(r.$ref.get) }
    case _ => None
  }

  private def extractParams(params: List[Either[Reference, Parameter]]): Seq[Parameter] =
    params.collect { case Right(p) => p }

  private def mergeParameters(pathLevel: Seq[Parameter], opLevel: Seq[Parameter]): Seq[Parameter] = {
    val opKeys = opLevel.map(p => (p.name, p.in)).toSet
    val inherited = pathLevel.filterNot(p => opKeys.contains((p.name, p.in)))
    inherited ++ opLevel
  }

  private[openapi] def convertParameter(p: Parameter): Option[ab.Parameter] = {
    if (p.in == ParameterIn.Header && filterHeadersLower.contains(p.name.toLowerCase)) return None
    mapLocation(p.in).map { location =>
      val schemaOpt = p.schema.collect { case s: Schema => s }

      val typeName = schemaOpt
        .map { s =>
          if (s.$ref.isDefined) resolve(refName(s.$ref.get))
          else SchemaConverter.simpleType(s).map(_.name).getOrElse(ScalarType.StringType.name)
        }
        .getOrElse(ScalarType.StringType.name)

      val (min, max) = schemaOpt.map(SchemaClassifier.extractBounds).getOrElse((None, None))
      val isRequired = p.required.getOrElse(p.in == ParameterIn.Path)
      val example = p.example.collect { case ExampleSingleValue(v) => v.toString }

      ab.Parameter(
        name = p.name,
        `type` = sn(typeName),
        location = location,
        description = p.description,
        deprecation = None,
        required = isRequired,
        default = None,
        minimum = min,
        maximum = max,
        example = example,
      )
    }
  }

  private def mapLocation(in: ParameterIn): Option[ab.ParameterLocation] = in match {
    case ParameterIn.Query => Some(ab.ParameterLocation.Query)
    case ParameterIn.Header => Some(ab.ParameterLocation.Header)
    case ParameterIn.Path => Some(ab.ParameterLocation.Path)
    case ParameterIn.Cookie => None // callers pre-filter and report cookie params
  }

  private def is2xx(code: ab.ResponseCode): Boolean = code match {
    case ab.ResponseCodeInt(c) => c >= 200 && c < 300
    case _ => false
  }

  private def formatResponseKey(key: ResponsesKey): String = key match {
    case ResponsesCodeKey(c) => c.toString
    case ResponsesDefaultKey => "default"
    case ResponsesRangeKey(r) => s"${r}xx"
  }

  private def toApibuilderPath(path: String): String =
    path.replaceAll("\\{([^}]+)\\}", ":$1")

  private def sn(str: String): String = uniqueSnakeCase(str, config)
  private def resolve(name: String): String = resolveReference(name, modelReferences) match {
    case Right(resolved) => resolved
    case Left(err) =>
      System.err.println(s"Warning: $err")
      name
  }
}
