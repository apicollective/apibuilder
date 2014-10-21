package com.gilt.apidocgenerator.models {
  case class Enum(
    name: String,
    description: scala.Option[String] = None,
    values: scala.collection.Seq[com.gilt.apidocgenerator.models.EnumValue]
  )

  case class EnumValue(
    name: String,
    description: scala.Option[String] = None
  )

  case class Error(
    code: String,
    message: String
  )

  case class Field(
    name: String,
    datatype: com.gilt.apidocgenerator.models.Type,
    description: scala.Option[String] = None,
    required: Boolean,
    default: scala.Option[String] = None,
    example: scala.Option[String] = None,
    minimum: scala.Option[Long] = None,
    maximum: scala.Option[Long] = None
  )

  /**
   * The generator metadata.
   */
  case class Generator(
    key: String,
    name: String,
    language: scala.Option[String] = None,
    description: scala.Option[String] = None
  )

  case class Header(
    name: String,
    headertype: com.gilt.apidocgenerator.models.HeaderType,
    headertypeValue: scala.Option[String] = None,
    description: scala.Option[String] = None,
    required: Boolean,
    multiple: Boolean,
    default: scala.Option[String] = None
  )

  /**
   * The result of invoking a generator.
   */
  case class Invocation(
    source: String
  )

  case class Model(
    name: String,
    plural: String,
    description: scala.Option[String] = None,
    fields: scala.collection.Seq[com.gilt.apidocgenerator.models.Field]
  )

  case class Operation(
    model: com.gilt.apidocgenerator.models.Model,
    method: String,
    path: String,
    description: scala.Option[String] = None,
    body: scala.Option[com.gilt.apidocgenerator.models.Type] = None,
    parameters: scala.collection.Seq[com.gilt.apidocgenerator.models.Parameter],
    responses: scala.collection.Seq[com.gilt.apidocgenerator.models.Response]
  )

  case class Parameter(
    name: String,
    datatype: com.gilt.apidocgenerator.models.Type,
    location: com.gilt.apidocgenerator.models.ParameterLocation,
    description: scala.Option[String] = None,
    required: Boolean,
    default: scala.Option[String] = None,
    example: scala.Option[String] = None,
    minimum: scala.Option[Long] = None,
    maximum: scala.Option[Long] = None
  )

  case class Resource(
    model: com.gilt.apidocgenerator.models.Model,
    path: String,
    operations: scala.collection.Seq[com.gilt.apidocgenerator.models.Operation]
  )

  case class Response(
    code: Int,
    datatype: com.gilt.apidocgenerator.models.Type
  )

  /**
   * Description of a service
   */
  case class ServiceDescription(
    enums: scala.collection.Seq[com.gilt.apidocgenerator.models.Enum],
    models: scala.collection.Seq[com.gilt.apidocgenerator.models.Model],
    headers: scala.collection.Seq[com.gilt.apidocgenerator.models.Header],
    resources: scala.collection.Seq[com.gilt.apidocgenerator.models.Resource],
    baseUrl: scala.Option[String] = None,
    name: String,
    packageName: scala.Option[String] = None,
    description: scala.Option[String] = None,
    userAgent: scala.Option[String] = None
  )

  case class Type(
    kind: com.gilt.apidocgenerator.models.TypeKind,
    name: String,
    multiple: Boolean
  )

  sealed trait HeaderType

  object HeaderType {

    case object String extends HeaderType { override def toString = "string" }
    case object Enum extends HeaderType { override def toString = "enum" }

    /**
     * UNDEFINED captures values that are sent either in error or
     * that were added by the server after this library was
     * generated. We want to make it easy and obvious for users of
     * this library to handle this case gracefully.
     *
     * We use all CAPS for the variable name to avoid collisions
     * with the camel cased values above.
     */
    case class UNDEFINED(override val toString: String) extends HeaderType

    /**
     * all returns a list of all the valid, known values. We use
     * lower case to avoid collisions with the camel cased values
     * above.
     */
    val all = Seq(String, Enum)

    private[this]
    val byName = all.map(x => x.toString -> x).toMap

    def apply(value: String): HeaderType = fromString(value).getOrElse(UNDEFINED(value))

    def fromString(value: String): scala.Option[HeaderType] = byName.get(value)

  }

  sealed trait ParameterLocation

  object ParameterLocation {

    case object Path extends ParameterLocation { override def toString = "path" }
    case object Query extends ParameterLocation { override def toString = "query" }
    case object Form extends ParameterLocation { override def toString = "form" }

    /**
     * UNDEFINED captures values that are sent either in error or
     * that were added by the server after this library was
     * generated. We want to make it easy and obvious for users of
     * this library to handle this case gracefully.
     *
     * We use all CAPS for the variable name to avoid collisions
     * with the camel cased values above.
     */
    case class UNDEFINED(override val toString: String) extends ParameterLocation

    /**
     * all returns a list of all the valid, known values. We use
     * lower case to avoid collisions with the camel cased values
     * above.
     */
    val all = Seq(Path, Query, Form)

    private[this]
    val byName = all.map(x => x.toString -> x).toMap

    def apply(value: String): ParameterLocation = fromString(value).getOrElse(UNDEFINED(value))

    def fromString(value: String): scala.Option[ParameterLocation] = byName.get(value)

  }

  sealed trait TypeKind

  object TypeKind {

    case object Primitive extends TypeKind { override def toString = "primitive" }
    case object Model extends TypeKind { override def toString = "model" }
    case object Enum extends TypeKind { override def toString = "enum" }

    /**
     * UNDEFINED captures values that are sent either in error or
     * that were added by the server after this library was
     * generated. We want to make it easy and obvious for users of
     * this library to handle this case gracefully.
     *
     * We use all CAPS for the variable name to avoid collisions
     * with the camel cased values above.
     */
    case class UNDEFINED(override val toString: String) extends TypeKind

    /**
     * all returns a list of all the valid, known values. We use
     * lower case to avoid collisions with the camel cased values
     * above.
     */
    val all = Seq(Primitive, Model, Enum)

    private[this]
    val byName = all.map(x => x.toString -> x).toMap

    def apply(value: String): TypeKind = fromString(value).getOrElse(UNDEFINED(value))

    def fromString(value: String): scala.Option[TypeKind] = byName.get(value)

  }
}

package com.gilt.apidocgenerator.models {
  package object json {
    import play.api.libs.json.__
    import play.api.libs.json.JsString
    import play.api.libs.json.Writes
    import play.api.libs.functional.syntax._

    private[apidocgenerator] implicit val jsonReadsUUID = __.read[String].map(java.util.UUID.fromString)

    private[apidocgenerator] implicit val jsonWritesUUID = new Writes[java.util.UUID] {
      def writes(x: java.util.UUID) = JsString(x.toString)
    }

    private[apidocgenerator] implicit val jsonReadsJodaDateTime = __.read[String].map { str =>
      import org.joda.time.format.ISODateTimeFormat.dateTimeParser
      dateTimeParser.parseDateTime(str)
    }

    private[apidocgenerator] implicit val jsonWritesJodaDateTime = new Writes[org.joda.time.DateTime] {
      def writes(x: org.joda.time.DateTime) = {
        import org.joda.time.format.ISODateTimeFormat.dateTime
        val str = dateTime.print(x)
        JsString(str)
      }
    }

    implicit val jsonReadsApidocGeneratorEnum_HeaderType = __.read[String].map(HeaderType.apply)
    implicit val jsonWritesApidocGeneratorEnum_HeaderType = new Writes[HeaderType] {
      def writes(x: HeaderType) = JsString(x.toString)
    }

    implicit val jsonReadsApidocGeneratorEnum_ParameterLocation = __.read[String].map(ParameterLocation.apply)
    implicit val jsonWritesApidocGeneratorEnum_ParameterLocation = new Writes[ParameterLocation] {
      def writes(x: ParameterLocation) = JsString(x.toString)
    }

    implicit val jsonReadsApidocGeneratorEnum_TypeKind = __.read[String].map(TypeKind.apply)
    implicit val jsonWritesApidocGeneratorEnum_TypeKind = new Writes[TypeKind] {
      def writes(x: TypeKind) = JsString(x.toString)
    }
    implicit def jsonReadsApidocGeneratorEnum: play.api.libs.json.Reads[Enum] = {
      (
        (__ \ "name").read[String] and
        (__ \ "description").readNullable[String] and
        (__ \ "values").readNullable[scala.collection.Seq[com.gilt.apidocgenerator.models.EnumValue]].map(_.getOrElse(Nil))
      )(Enum.apply _)
    }

    implicit def jsonWritesApidocGeneratorEnum: play.api.libs.json.Writes[Enum] = {
      (
        (__ \ "name").write[String] and
        (__ \ "description").write[scala.Option[String]] and
        (__ \ "values").write[scala.collection.Seq[com.gilt.apidocgenerator.models.EnumValue]]
      )(unlift(Enum.unapply _))
    }

    implicit def jsonReadsApidocGeneratorEnumValue: play.api.libs.json.Reads[EnumValue] = {
      (
        (__ \ "name").read[String] and
        (__ \ "description").readNullable[String]
      )(EnumValue.apply _)
    }

    implicit def jsonWritesApidocGeneratorEnumValue: play.api.libs.json.Writes[EnumValue] = {
      (
        (__ \ "name").write[String] and
        (__ \ "description").write[scala.Option[String]]
      )(unlift(EnumValue.unapply _))
    }

    implicit def jsonReadsApidocGeneratorError: play.api.libs.json.Reads[Error] = {
      (
        (__ \ "code").read[String] and
        (__ \ "message").read[String]
      )(Error.apply _)
    }

    implicit def jsonWritesApidocGeneratorError: play.api.libs.json.Writes[Error] = {
      (
        (__ \ "code").write[String] and
        (__ \ "message").write[String]
      )(unlift(Error.unapply _))
    }

    implicit def jsonReadsApidocGeneratorField: play.api.libs.json.Reads[Field] = {
      (
        (__ \ "name").read[String] and
        (__ \ "datatype").read[com.gilt.apidocgenerator.models.Type] and
        (__ \ "description").readNullable[String] and
        (__ \ "required").read[Boolean] and
        (__ \ "default").readNullable[String] and
        (__ \ "example").readNullable[String] and
        (__ \ "minimum").readNullable[Long] and
        (__ \ "maximum").readNullable[Long]
      )(Field.apply _)
    }

    implicit def jsonWritesApidocGeneratorField: play.api.libs.json.Writes[Field] = {
      (
        (__ \ "name").write[String] and
        (__ \ "datatype").write[com.gilt.apidocgenerator.models.Type] and
        (__ \ "description").write[scala.Option[String]] and
        (__ \ "required").write[Boolean] and
        (__ \ "default").write[scala.Option[String]] and
        (__ \ "example").write[scala.Option[String]] and
        (__ \ "minimum").write[scala.Option[Long]] and
        (__ \ "maximum").write[scala.Option[Long]]
      )(unlift(Field.unapply _))
    }

    implicit def jsonReadsApidocGeneratorGenerator: play.api.libs.json.Reads[Generator] = {
      (
        (__ \ "key").read[String] and
        (__ \ "name").read[String] and
        (__ \ "language").readNullable[String] and
        (__ \ "description").readNullable[String]
      )(Generator.apply _)
    }

    implicit def jsonWritesApidocGeneratorGenerator: play.api.libs.json.Writes[Generator] = {
      (
        (__ \ "key").write[String] and
        (__ \ "name").write[String] and
        (__ \ "language").write[scala.Option[String]] and
        (__ \ "description").write[scala.Option[String]]
      )(unlift(Generator.unapply _))
    }

    implicit def jsonReadsApidocGeneratorHeader: play.api.libs.json.Reads[Header] = {
      (
        (__ \ "name").read[String] and
        (__ \ "headertype").read[com.gilt.apidocgenerator.models.HeaderType] and
        (__ \ "headertype_value").readNullable[String] and
        (__ \ "description").readNullable[String] and
        (__ \ "required").read[Boolean] and
        (__ \ "multiple").read[Boolean] and
        (__ \ "default").readNullable[String]
      )(Header.apply _)
    }

    implicit def jsonWritesApidocGeneratorHeader: play.api.libs.json.Writes[Header] = {
      (
        (__ \ "name").write[String] and
        (__ \ "headertype").write[com.gilt.apidocgenerator.models.HeaderType] and
        (__ \ "headertype_value").write[scala.Option[String]] and
        (__ \ "description").write[scala.Option[String]] and
        (__ \ "required").write[Boolean] and
        (__ \ "multiple").write[Boolean] and
        (__ \ "default").write[scala.Option[String]]
      )(unlift(Header.unapply _))
    }

    implicit def jsonReadsApidocGeneratorInvocation: play.api.libs.json.Reads[Invocation] = {
      (__ \ "source").read[String].map { x => new Invocation(source = x) }
    }

    implicit def jsonWritesApidocGeneratorInvocation: play.api.libs.json.Writes[Invocation] = new play.api.libs.json.Writes[Invocation] {
      def writes(x: Invocation) = play.api.libs.json.Json.obj(
        "source" -> play.api.libs.json.Json.toJson(x.source)
      )
    }

    implicit def jsonReadsApidocGeneratorModel: play.api.libs.json.Reads[Model] = {
      (
        (__ \ "name").read[String] and
        (__ \ "plural").read[String] and
        (__ \ "description").readNullable[String] and
        (__ \ "fields").readNullable[scala.collection.Seq[com.gilt.apidocgenerator.models.Field]].map(_.getOrElse(Nil))
      )(Model.apply _)
    }

    implicit def jsonWritesApidocGeneratorModel: play.api.libs.json.Writes[Model] = {
      (
        (__ \ "name").write[String] and
        (__ \ "plural").write[String] and
        (__ \ "description").write[scala.Option[String]] and
        (__ \ "fields").write[scala.collection.Seq[com.gilt.apidocgenerator.models.Field]]
      )(unlift(Model.unapply _))
    }

    implicit def jsonReadsApidocGeneratorOperation: play.api.libs.json.Reads[Operation] = {
      (
        (__ \ "model").read[com.gilt.apidocgenerator.models.Model] and
        (__ \ "method").read[String] and
        (__ \ "path").read[String] and
        (__ \ "description").readNullable[String] and
        (__ \ "body").readNullable[com.gilt.apidocgenerator.models.Type] and
        (__ \ "parameters").readNullable[scala.collection.Seq[com.gilt.apidocgenerator.models.Parameter]].map(_.getOrElse(Nil)) and
        (__ \ "responses").readNullable[scala.collection.Seq[com.gilt.apidocgenerator.models.Response]].map(_.getOrElse(Nil))
      )(Operation.apply _)
    }

    implicit def jsonWritesApidocGeneratorOperation: play.api.libs.json.Writes[Operation] = {
      (
        (__ \ "model").write[com.gilt.apidocgenerator.models.Model] and
        (__ \ "method").write[String] and
        (__ \ "path").write[String] and
        (__ \ "description").write[scala.Option[String]] and
        (__ \ "body").write[scala.Option[com.gilt.apidocgenerator.models.Type]] and
        (__ \ "parameters").write[scala.collection.Seq[com.gilt.apidocgenerator.models.Parameter]] and
        (__ \ "responses").write[scala.collection.Seq[com.gilt.apidocgenerator.models.Response]]
      )(unlift(Operation.unapply _))
    }

    implicit def jsonReadsApidocGeneratorParameter: play.api.libs.json.Reads[Parameter] = {
      (
        (__ \ "name").read[String] and
        (__ \ "datatype").read[com.gilt.apidocgenerator.models.Type] and
        (__ \ "location").read[com.gilt.apidocgenerator.models.ParameterLocation] and
        (__ \ "description").readNullable[String] and
        (__ \ "required").read[Boolean] and
        (__ \ "default").readNullable[String] and
        (__ \ "example").readNullable[String] and
        (__ \ "minimum").readNullable[Long] and
        (__ \ "maximum").readNullable[Long]
      )(Parameter.apply _)
    }

    implicit def jsonWritesApidocGeneratorParameter: play.api.libs.json.Writes[Parameter] = {
      (
        (__ \ "name").write[String] and
        (__ \ "datatype").write[com.gilt.apidocgenerator.models.Type] and
        (__ \ "location").write[com.gilt.apidocgenerator.models.ParameterLocation] and
        (__ \ "description").write[scala.Option[String]] and
        (__ \ "required").write[Boolean] and
        (__ \ "default").write[scala.Option[String]] and
        (__ \ "example").write[scala.Option[String]] and
        (__ \ "minimum").write[scala.Option[Long]] and
        (__ \ "maximum").write[scala.Option[Long]]
      )(unlift(Parameter.unapply _))
    }

    implicit def jsonReadsApidocGeneratorResource: play.api.libs.json.Reads[Resource] = {
      (
        (__ \ "model").read[com.gilt.apidocgenerator.models.Model] and
        (__ \ "path").read[String] and
        (__ \ "operations").readNullable[scala.collection.Seq[com.gilt.apidocgenerator.models.Operation]].map(_.getOrElse(Nil))
      )(Resource.apply _)
    }

    implicit def jsonWritesApidocGeneratorResource: play.api.libs.json.Writes[Resource] = {
      (
        (__ \ "model").write[com.gilt.apidocgenerator.models.Model] and
        (__ \ "path").write[String] and
        (__ \ "operations").write[scala.collection.Seq[com.gilt.apidocgenerator.models.Operation]]
      )(unlift(Resource.unapply _))
    }

    implicit def jsonReadsApidocGeneratorResponse: play.api.libs.json.Reads[Response] = {
      (
        (__ \ "code").read[Int] and
        (__ \ "datatype").read[com.gilt.apidocgenerator.models.Type]
      )(Response.apply _)
    }

    implicit def jsonWritesApidocGeneratorResponse: play.api.libs.json.Writes[Response] = {
      (
        (__ \ "code").write[Int] and
        (__ \ "datatype").write[com.gilt.apidocgenerator.models.Type]
      )(unlift(Response.unapply _))
    }

    implicit def jsonReadsApidocGeneratorServiceDescription: play.api.libs.json.Reads[ServiceDescription] = {
      (
        (__ \ "enums").readNullable[scala.collection.Seq[com.gilt.apidocgenerator.models.Enum]].map(_.getOrElse(Nil)) and
        (__ \ "models").readNullable[scala.collection.Seq[com.gilt.apidocgenerator.models.Model]].map(_.getOrElse(Nil)) and
        (__ \ "headers").readNullable[scala.collection.Seq[com.gilt.apidocgenerator.models.Header]].map(_.getOrElse(Nil)) and
        (__ \ "resources").readNullable[scala.collection.Seq[com.gilt.apidocgenerator.models.Resource]].map(_.getOrElse(Nil)) and
        (__ \ "baseUrl").readNullable[String] and
        (__ \ "name").read[String] and
        (__ \ "package_name").readNullable[String] and
        (__ \ "description").readNullable[String] and
        (__ \ "user_agent").readNullable[String]
      )(ServiceDescription.apply _)
    }

    implicit def jsonWritesApidocGeneratorServiceDescription: play.api.libs.json.Writes[ServiceDescription] = {
      (
        (__ \ "enums").write[scala.collection.Seq[com.gilt.apidocgenerator.models.Enum]] and
        (__ \ "models").write[scala.collection.Seq[com.gilt.apidocgenerator.models.Model]] and
        (__ \ "headers").write[scala.collection.Seq[com.gilt.apidocgenerator.models.Header]] and
        (__ \ "resources").write[scala.collection.Seq[com.gilt.apidocgenerator.models.Resource]] and
        (__ \ "baseUrl").write[scala.Option[String]] and
        (__ \ "name").write[String] and
        (__ \ "package_name").write[scala.Option[String]] and
        (__ \ "description").write[scala.Option[String]] and
        (__ \ "user_agent").write[scala.Option[String]]
      )(unlift(ServiceDescription.unapply _))
    }

    implicit def jsonReadsApidocGeneratorType: play.api.libs.json.Reads[Type] = {
      (
        (__ \ "kind").read[com.gilt.apidocgenerator.models.TypeKind] and
        (__ \ "name").read[String] and
        (__ \ "multiple").read[Boolean]
      )(Type.apply _)
    }

    implicit def jsonWritesApidocGeneratorType: play.api.libs.json.Writes[Type] = {
      (
        (__ \ "kind").write[com.gilt.apidocgenerator.models.TypeKind] and
        (__ \ "name").write[String] and
        (__ \ "multiple").write[Boolean]
      )(unlift(Type.unapply _))
    }
  }
}

package com.gilt.apidocgenerator {

  class Client(apiUrl: String, apiToken: scala.Option[String] = None) {
    import com.gilt.apidocgenerator.models.json._

    private val UserAgent = "apidoc:0.6.8 http://www.apidoc.me/gilt/code/apidoc-generator/0.7.1-dev/play_2_3_client"
    private val logger = play.api.Logger("com.gilt.apidocgenerator.client")

    logger.info(s"Initializing com.gilt.apidocgenerator.client for url $apiUrl")

    def generators: Generators = Generators

    def invocations: Invocations = Invocations

    object Generators extends Generators {
      override def get()(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[com.gilt.apidocgenerator.models.Generator]] = {
        _executeRequest("GET", s"/generators").map {
          case r if r.status == 200 => r.json.as[scala.collection.Seq[com.gilt.apidocgenerator.models.Generator]]
          case r => throw new FailedRequest(r)
        }
      }

      override def getByKey(
        key: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[com.gilt.apidocgenerator.models.Generator]] = {
        _executeRequest("GET", s"/generators/${play.utils.UriEncoding.encodePathSegment(key, "UTF-8")}").map {
          case r if r.status == 200 => Some(r.json.as[com.gilt.apidocgenerator.models.Generator])
          case r if r.status == 404 => None
          case r => throw new FailedRequest(r)
        }
      }
    }

    object Invocations extends Invocations {
      override def postByKey(serviceDescription: com.gilt.apidocgenerator.models.ServiceDescription,
        key: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.apidocgenerator.models.Invocation] = {
        val payload = play.api.libs.json.Json.toJson(serviceDescription)

        _executeRequest("POST", s"/invocations/${play.utils.UriEncoding.encodePathSegment(key, "UTF-8")}", body = Some(payload)).map {
          case r if r.status == 200 => r.json.as[com.gilt.apidocgenerator.models.Invocation]
          case r if r.status == 409 => throw new com.gilt.apidocgenerator.error.ErrorsResponse(r)
          case r => throw new FailedRequest(r)
        }
      }
    }

    def _requestHolder(path: String): play.api.libs.ws.WSRequestHolder = {
      import play.api.Play.current

      val holder = play.api.libs.ws.WS.url(apiUrl + path).withHeaders("User-Agent" -> UserAgent)
      apiToken.fold(holder) { token =>
        holder.withAuth(token, "", play.api.libs.ws.WSAuthScheme.BASIC)
      }
    }

    def _logRequest(method: String, req: play.api.libs.ws.WSRequestHolder)(implicit ec: scala.concurrent.ExecutionContext): play.api.libs.ws.WSRequestHolder = {
      val queryComponents = for {
        (name, values) <- req.queryString
        value <- values
      } yield name -> value
      val url = s"${req.url}${queryComponents.mkString("?", "&", "")}"
      apiToken.fold(logger.info(s"curl -X $method $url")) { _ =>
        logger.info(s"curl -X $method -u '[REDACTED]:' $url")
      }
      req
    }

    def _executeRequest(
      method: String,
      path: String,
      queryParameters: Seq[(String, String)] = Seq.empty,
      body: Option[play.api.libs.json.JsValue] = None
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      method.toUpperCase match {
        case "GET" => {
          _logRequest("GET", _requestHolder(path).withQueryString(queryParameters:_*)).get()
        }
        case "POST" => {
          _logRequest("POST", _requestHolder(path).withQueryString(queryParameters:_*)).post(body.getOrElse(play.api.libs.json.Json.obj()))
        }
        case "PUT" => {
          _logRequest("PUT", _requestHolder(path).withQueryString(queryParameters:_*)).put(body.getOrElse(play.api.libs.json.Json.obj()))
        }
        case "PATCH" => {
          _logRequest("PATCH", _requestHolder(path).withQueryString(queryParameters:_*)).patch(body.getOrElse(play.api.libs.json.Json.obj()))
        }
        case "DELETE" => {
          _logRequest("DELETE", _requestHolder(path).withQueryString(queryParameters:_*)).delete()
        }
        case _ => {
          _logRequest(method, _requestHolder(path).withQueryString(queryParameters:_*))
          sys.error("Unsupported method[%s]".format(method))
        }
      }
    }

  }

  trait Generators {
    /**
     * Get all available generators
     */
    def get()(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[com.gilt.apidocgenerator.models.Generator]]

    /**
     * Get the meta data of this generator
     */
    def getByKey(
      key: String
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[com.gilt.apidocgenerator.models.Generator]]
  }

  trait Invocations {
    /**
     * Invoke a generator
     */
    def postByKey(serviceDescription: com.gilt.apidocgenerator.models.ServiceDescription,
      key: String
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.apidocgenerator.models.Invocation]
  }

  case class FailedRequest(
    response: play.api.libs.ws.WSResponse,
    message: Option[String] = None
  ) extends Exception(message.getOrElse(response.status + ": " + response.body))

  package error {

    import com.gilt.apidocgenerator.models.json._

    case class ErrorsResponse(
      response: play.api.libs.ws.WSResponse,
      message: Option[String] = None
    ) extends Exception(message.getOrElse(response.status + ": " + response.body)){
      import com.gilt.apidocgenerator.models.json._
      lazy val errors = response.json.as[scala.collection.Seq[com.gilt.apidocgenerator.models.Error]]
    }
  }

  object Bindables {

    import play.api.mvc.{PathBindable, QueryStringBindable}
    import org.joda.time.{DateTime, LocalDate}
    import org.joda.time.format.ISODateTimeFormat
    import com.gilt.apidocgenerator.models._

    // Type: date-time-iso8601
    implicit val pathBindableTypeDateTimeIso8601 = new PathBindable.Parsing[DateTime](
      ISODateTimeFormat.dateTimeParser.parseDateTime(_), _.toString, (key: String, e: Exception) => s"Error parsing date time $key. Example: 2014-04-29T11:56:52Z"
    )

    // Type: date-iso8601
    implicit val pathBindableTypeDateIso8601 = new PathBindable.Parsing[LocalDate](
      ISODateTimeFormat.yearMonthDay.parseLocalDate(_), _.toString, (key: String, e: Exception) => s"Error parsing date time $key. Example: 2014-04-29"
    )

    // Enum: HeaderType
    private val enumHeaderTypeNotFound = (key: String, e: Exception) => s"Unrecognized $key, should be one of ${HeaderType.all.mkString(", ")}"

    implicit val pathBindableEnumHeaderType = new PathBindable.Parsing[HeaderType] (
      HeaderType.fromString(_).get, _.toString, enumHeaderTypeNotFound
    )

    implicit val queryStringBindableEnumHeaderType = new QueryStringBindable.Parsing[HeaderType](
      HeaderType.fromString(_).get, _.toString, enumHeaderTypeNotFound
    )

    // Enum: ParameterLocation
    private val enumParameterLocationNotFound = (key: String, e: Exception) => s"Unrecognized $key, should be one of ${ParameterLocation.all.mkString(", ")}"

    implicit val pathBindableEnumParameterLocation = new PathBindable.Parsing[ParameterLocation] (
      ParameterLocation.fromString(_).get, _.toString, enumParameterLocationNotFound
    )

    implicit val queryStringBindableEnumParameterLocation = new QueryStringBindable.Parsing[ParameterLocation](
      ParameterLocation.fromString(_).get, _.toString, enumParameterLocationNotFound
    )

    // Enum: TypeKind
    private val enumTypeKindNotFound = (key: String, e: Exception) => s"Unrecognized $key, should be one of ${TypeKind.all.mkString(", ")}"

    implicit val pathBindableEnumTypeKind = new PathBindable.Parsing[TypeKind] (
      TypeKind.fromString(_).get, _.toString, enumTypeKindNotFound
    )

    implicit val queryStringBindableEnumTypeKind = new QueryStringBindable.Parsing[TypeKind](
      TypeKind.fromString(_).get, _.toString, enumTypeKindNotFound
    )

  }

}
