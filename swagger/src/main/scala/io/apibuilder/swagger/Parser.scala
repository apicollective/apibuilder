package io.apibuilder.swagger

import java.io.File
import io.apibuilder.spec.v0.models._
import io.swagger.models.parameters.AbstractSerializableParameter
import io.swagger.models.properties.{ArrayProperty, Property, RefProperty}
import io.swagger.models.{Info => _, Model => _, _}
import io.swagger.{models => swagger}
import io.swagger.parser.SwaggerParser
import lib.{ServiceConfiguration, Text, UrlKey}
import io.apibuilder.swagger.translators.Resolver

import scala.annotation.{nowarn, tailrec}
import scala.jdk.CollectionConverters._

case class Parser(config: ServiceConfiguration) {

  def parseString(
    contents: String
  ): Service = {
    parse(Util.writeToTempFile(contents))
  }

  def parse(
    path: File
  ): Service = {
    val swagger = Option(new SwaggerParser().read(path.toString)).getOrElse {
      sys.error("File is not a valid Swagger JSON or YAML format")
    }
    val info = swagger.getInfo // TODO
    val applicationKey = UrlKey.generate(info.getTitle)
    val specModelsAndEnums = parseDefinitions(swagger)
    val specModels = specModelsAndEnums._1
    val specEnums = specModelsAndEnums._2
    val resolver = Resolver(models = specModels, enums = specEnums)
    val resourceAndEnums = parseResources(swagger, resolver)
    val resources = resourceAndEnums.map(_.resource)
    val paramEnums = resourceAndEnums.flatMap(_.`enum`).distinct
    val finalModels = specModels ++ findPlaceholder(resources).toSeq

    Service(
      name = info.getTitle,
      info = Info(
        contact = None,
        license = None
      ),
      description = Option(info.getDescription),
      baseUrl = translators.BaseUrl(Util.toArray(swagger.getSchemes).map(_.toString), swagger.getHost, Option(swagger.getBasePath)).headOption,
      namespace = config.applicationNamespace(applicationKey),
      organization = Organization(key = config.orgKey),
      application = Application(key = applicationKey),
      version = config.version,
      enums = (specEnums ++ paramEnums).distinct,
      unions = Nil,
      models = finalModels,
      imports = Nil,
      headers = Nil,
      resources = translators.Resource.mergeAll(resources),
      attributes =
        Seq(
          SwaggerData(
            externalDocs = swagger.getExternalDocs,
            serviceSecurity = swagger.getSecurity,
            securityDefinitions = swagger.getSecurityDefinitions,
            schemes = swagger.getSchemes,
            host = swagger.getHost,
            basePath = swagger.getBasePath
          ).toAttribute
        ).flatten ++ Util.vendorExtensionsToAttributes(swagger.getVendorExtensions)
    )
  }

  private def findPlaceholder(resources: Seq[Resource]): Option[Model] = {
    if (resources.exists { r =>
      r.`type` == translators.Model.Placeholder.name
    }) {
      Some(translators.Model.Placeholder)
    } else {
      None
    }
  }

  private def parseDefinitions(swagger: Swagger): (Seq[Model], Seq[Enum]) = {
    buildModels(
      selector = ModelSelector(Util.toMap(swagger.getDefinitions)),
      resolver = Resolver(models = Nil, enums = Nil)
    )
  }

  @tailrec
  private def buildModels(
    selector: ModelSelector,
    resolver: Resolver
  ): (Seq[Model], Seq[Enum]) = {
    selector.next() match {
      case None => {
        selector.remaining().toList match {
          case Nil => (resolver.models, resolver.enums)
          case remaining => sys.error("Failed to resolve definitions: " + remaining.map(_.name).mkString(", "))
        }
      }

      case Some(mydefinition) => {
        var newEnums = Seq[Enum]()
        val name = mydefinition.name
        val newModelOpt = mydefinition.definition match {
          case m: ComposedModel => {
            var composedModel: Option[Model] = None

            m.getAllOf.asScala.foreach { swaggerModel =>
              val thisModelOpt = swaggerModel match {
                case m: RefModel =>
                  Some(resolver.resolveWithError(m))
                case m: ModelImpl =>
                  val translated = translators.Model(resolver, name, m)
                  newEnums ++= translated._2
                  translated._1
                case _ =>
                  sys.error(s"Unsupported composition model[$name] - $swaggerModel")
              }

              composedModel = composedModel match {
                case None => thisModelOpt map {thisModel =>
                  thisModel.copy(
                    name = name,
                    plural = Text.pluralize(name)
                  )
                }
                case Some(cm) => thisModelOpt map { thisModel => translators.Model.compose(cm, thisModel)}
              }
            }

            if(composedModel.isEmpty)
              sys.error(s"Empty composed model: $name")
            composedModel
          }

          case rm: RefModel => Some(resolver.resolveWithError(rm))
          case m: ModelImpl => {
            val translated = translators.Model(resolver, name, m)
            newEnums ++= translated._2
            translated._1
          }
          case _: ArrayModel => sys.error(s"Unsupported definition for name[$name]. Array models are not supported - please see https://github.com/apicollective/apibuilder/blob/main/SWAGGER.md")
          case _ => sys.error(s"Unsupported definition for name[$name]")
        }

        buildModels(
          selector = selector,
          resolver =
            Resolver(
              models  = resolver.models ++ newModelOpt.map(Seq(_)).getOrElse(Seq()),
              enums   = resolver.enums ++ newEnums)
        )
      }
    }
  }

  private def retrieveModelProperty(schema: Property): Option[Property] = {
    Option(schema).flatMap {
      case array: ArrayProperty => retrieveModelProperty(array.getItems)
      case ref: RefProperty => Some(ref)
      case _ => None
    }
  }

  /**
   * Selects the first successful response by looking first for '*' and then the lowest response code in
   * the range [200, 300)
   */
  private def selectSuccessfulResponse(responses: Map[String, swagger.Response]): Option[swagger.Response] = {
    responses.get("*").orElse {
      responses.keys.flatMap(_.toIntOption)
        .filter(_ >= 200)
        .filter(_ < 300)
        .toList
        .sorted.headOption.flatMap { c => responses.get(c.toString) }
    }
  }

  private case class PathWithUrl(path: Path, url: String)
  private case class SwaggerResponse(pathWithUrl: PathWithUrl, url: String, operation: swagger.Operation, response: swagger.Response)
  private case class ResourceWithEnums(resource: Resource, `enum`: Seq[Enum])

  @nowarn
  private def parseResources(
    swagger: Swagger,
    resolver: Resolver
  ): Seq[ResourceWithEnums] = {
    val allPaths = swagger.getPaths.asScala
    val withResponse = allPaths.keys.toList.sortBy(_.length).flatMap { url =>
      val p = allPaths(url)
      p.getOperations.asScala.flatMap { op =>
        selectSuccessfulResponse(op.getResponses.asScala.toMap).map { r =>
          SwaggerResponse(PathWithUrl(p, url), url, op, r)
        }
      }
    }.groupBy(_.pathWithUrl)
    withResponse.keys.toList.sortBy(_.url.length).map { pathWithUrl =>
      val path = pathWithUrl.path
      val responses = withResponse(pathWithUrl)
      val commonModel = responses.flatMap { resp =>
        retrieveModelProperty(resp.response.getSchema).filter(_ != null)
      }.distinct match {
        case Nil => None
        case one :: Nil => Some(one)
        case mult => sys.error(s"Multiple models found for path: $path: ${mult.mkString(", ")}")
      }

      val enums = buildEnums(path, commonModel)
      val apiBuilderModel = findModelForResource(resolver, commonModel).getOrElse(translators.Model.Placeholder)
      val resource = translators.Resource(
        resolver.copy(enums = resolver.enums ++ enums), apiBuilderModel, responses.head.url, path,
      )
      ResourceWithEnums(resource, enums)
    }
  }

  private def buildEnums(path: swagger.Path, model: Option[swagger.properties.Property]): Seq[Enum] = {
    model match {
      case Some(ref: RefProperty) => {
        //Search for param enums among all operations (even the ones with no 200 response) of this resource
        path.getOperations.asScala.toSeq.flatMap { op =>
          op.getParameters.asScala.filter(Util.hasStringEnum).map { param =>
            val httpMethod = Util.retrieveMethod(op, path).get
            val enumTypeName = Util.buildParamEnumTypeName(ref.getSimpleRef, param, httpMethod.toString)
            Enum(
              name = enumTypeName,
              plural = Text.pluralize(enumTypeName),
              description = None,
              deprecation = None,
              values = param.asInstanceOf[AbstractSerializableParameter[_]].getEnum.asScala.map { value =>
                EnumValue(name = value, description = None, deprecation = None, attributes = Seq())
              }.toSeq,
              attributes = Seq(),
            )
          }
        }
      }
      case _ => Nil
    }
  }

  private def findModelForResource(resolver: Resolver, model: Option[swagger.properties.Property]) = {
    model match {
      case Some(ref: RefProperty) => {
        resolver.findModelByOkResponseSchema(ref.getSimpleRef)
      }
      case _ => None
    }
  }

}

