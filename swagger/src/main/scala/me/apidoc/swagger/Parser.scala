package me.apidoc.swagger

import translators.Resolver
import lib.{ServiceConfiguration, Text, UrlKey}
import java.io.File
import scala.collection.JavaConversions._
import play.api.libs.json.{Json, JsArray, JsObject, JsString, JsValue}

import io.swagger.parser.SwaggerParser
import com.wordnik.swagger.models.{ComposedModel, ModelImpl, RefModel, Swagger}
import com.wordnik.swagger.models.{parameters => swaggerparams}
import com.gilt.apidoc.spec.v0.models._
import com.wordnik.swagger.models.properties.Property

import lib.Text
import com.gilt.apidoc.spec.v0.models._
import scala.annotation.tailrec

case class Parser(config: ServiceConfiguration) {

  def parseString(
    contents: String
  ): Service = {
    parse(Util.writeToTempFile(contents))
  }

  def parse(
    path: File
  ): Service = {
    val swagger = new SwaggerParser().read(path.toString)
    val info = swagger.getInfo()
    val applicationKey = UrlKey.generate(info.getTitle())
    val specModels = models(swagger)
    val resolver = Resolver(models = specModels)

    Service(
      name = info.getTitle(),
      description = Option(info.getDescription()),
      baseUrl = translators.BaseUrl(Util.toArray(swagger.getSchemes).map(_.toString), swagger.getHost, Option(swagger.getBasePath)).headOption,
      namespace = config.applicationNamespace(applicationKey),
      organization = Organization(key = config.orgKey),
      application = Application(key = applicationKey),
      version = config.version,
      enums = Nil,
      unions = Nil,
      models = specModels,
      imports = Nil,
      headers = Nil,
      resources = translators.Resource.mergeAll(resources(swagger, resolver))
    )
  }

  private def models(swagger: Swagger): Seq[Model] = {
    buildModels(
      selector = ModelSelector(swagger.getDefinitions.toMap),
      resolver = Resolver(models = Nil)
    )
  }

  @tailrec
  private def buildModels(
    selector: ModelSelector,
    resolver: Resolver
  ): Seq[Model] = {
    selector.next() match {
      case None => {
        selector.remaining.toList match {
          case Nil => resolver.models
          case remaining => sys.error("Failed to resolve definitions: " + selector.remaining.map(_.name).mkString(", "))
        }
      }

      case Some(mydefinition) => {
        val name = mydefinition.name
        val newModel = mydefinition.definition match {
          case m: ComposedModel => {
            var composedModel: Option[Model] = None

            m.getInterfaces.foreach { i =>
              sys.error("TODO: Handle interfaces: " + i)
            }

            m.getAllOf.foreach { swaggerModel =>
              val thisModel = swaggerModel match {
                case rm: RefModel => resolver.resolveWithError(rm)
                case m: ModelImpl => translators.Model(resolver, name, m)
                case _ => sys.error(s"Unsupported composition model[$swaggerModel]")
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
          case m: ModelImpl => translators.Model(resolver, name, m)
          case _ => sys.error(s"Unsupported definition for name[$name]")
        }

        buildModels(
          selector = selector,
          resolver = Resolver(models = resolver.models ++ Seq(newModel))
        )
      }
    }
  }

  private def resources(
    swagger: Swagger,
    resolver: Resolver
  ): Seq[Resource] = {
    swagger.getPaths.map {
      case (url, p) => {
        resolver.findModelByUrl(url) match {
          case Some(model) => translators.Resource(resolver, model, url, p)
          case None => sys.error(s"Could not find model at url[$url]")
        }
      }
    }.toSeq
  }

}
