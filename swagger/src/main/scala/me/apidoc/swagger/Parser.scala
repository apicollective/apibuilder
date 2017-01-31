package me.apidoc.swagger

import translators.Resolver
import lib.{ServiceConfiguration, Text, UrlKey}
import io.swagger.parser.SwaggerParser
import com.wordnik.swagger.models.{ComposedModel, ModelImpl, RefModel, Swagger}
import java.io.File

import scala.collection.JavaConversions._
import com.bryzek.apidoc.spec.v0.models._

import scala.annotation.tailrec
import com.wordnik.swagger.models.properties.{ArrayProperty, RefProperty}

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
      sys.error("File is not a valid swagger.json format")
    }
    val info = swagger.getInfo() // TODO
    val applicationKey = UrlKey.generate(info.getTitle())
    val specModelsAndEnums = models(swagger)
    val specModels = specModelsAndEnums._1
    val resolver = Resolver(models = specModels, enums = specModelsAndEnums._2)

    Service(
      apidoc = Apidoc(version = com.bryzek.apidoc.spec.v0.Constants.Version),
      name = info.getTitle(),
      info = Info(
        contact = None,
        license = None
      ),
      description = Option(info.getDescription()),
      baseUrl = translators.BaseUrl(Util.toArray(swagger.getSchemes).map(_.toString), swagger.getHost, Option(swagger.getBasePath)).headOption,
      namespace = config.applicationNamespace(applicationKey),
      organization = Organization(key = config.orgKey),
      application = Application(key = applicationKey),
      version = config.version,
      enums = specModelsAndEnums._2,
      unions = Nil,
      models = specModels,
      imports = Nil,
      headers = Nil,
      resources = translators.Resource.mergeAll(resources(swagger, resolver))
    )
  }

  private def models(swagger: Swagger): (Seq[Model], Seq[Enum]) = {
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
        selector.remaining.toList match {
          case Nil => (resolver.models, resolver.enums)
          case remaining => sys.error("Failed to resolve definitions: " + selector.remaining.map(_.name).mkString(", "))
        }
      }

      case Some(mydefinition) => {
        var newEnums = Seq[Enum]()
        val name = mydefinition.name
        val newModel = mydefinition.definition match {
          case m: ComposedModel => {
            var composedModel: Option[Model] = None

            m.getInterfaces.foreach { i =>
              sys.error(s"TODO: Handle interfaces for definition[$name]: $i")
            }

            m.getAllOf.foreach { swaggerModel =>
              val thisModel = swaggerModel match {
                case m: RefModel => resolver.resolveWithError(m)
                case m: ModelImpl => {
                  val translated = translators.Model(resolver, name, m)
                  newEnums ++= translated._2
                  translated._1
                }
                case _ => sys.error(s"Unsupported composition model[$name] - $swaggerModel")
              }

              composedModel = composedModel match {
                case None => Some(
                  thisModel.copy(
                    name = name,
                    plural = Text.pluralize(name)
                  )
                )
                case Some(cm) => Some(translators.Model.compose(cm, thisModel))
              }
            }

            composedModel.getOrElse {
              sys.error(s"Empty composed model: $name")
            }
          }

          case rm: RefModel => resolver.resolveWithError(rm)
          case m: ModelImpl => {
            val translated = translators.Model(resolver, name, m)
            newEnums ++= translated._2
            translated._1
          }
          case _ => sys.error(s"Unsupported definition for name[$name]")
        }

        buildModels(
          selector = selector,
          resolver = Resolver(models = resolver.models ++ Seq(newModel), enums = resolver.enums ++ newEnums)
        )
      }
    }
  }

  private def resources(
    swagger: Swagger,
    resolver: Resolver
  ): Seq[Resource] =
    (for {
      (url, p) <- swagger.getPaths
      operation <- p.getOperations
      response <- operation.getResponses.toMap.get("200")
    } yield {
      val model = response.getSchema match {
        case array: ArrayProperty =>
          array.getItems
        case ref: RefProperty =>
          ref
      }
      model match {
        case ref: RefProperty =>
          resolver.findModelByOkResponseSchema(ref.getSimpleRef) match {
            case Some(model) => translators.Resource(resolver, model, url, p)
            case None => sys.error(s"Could not find model at url[$url]")
          }
      }
    }) toSeq

}
