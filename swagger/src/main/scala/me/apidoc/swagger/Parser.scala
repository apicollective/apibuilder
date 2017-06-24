package me.apidoc.swagger

import java.io.File

import io.apibuilder.apidoc.spec.v0.models._
import io.swagger.models.parameters.AbstractSerializableParameter
import io.swagger.models.properties.{ArrayProperty, RefProperty}
import io.swagger.models.{ComposedModel, ModelImpl, RefModel, Swagger}
import io.swagger.parser.SwaggerParser
import lib.{ServiceConfiguration, Text, UrlKey}
import me.apidoc.swagger.translators.Resolver

import scala.annotation.tailrec
import scala.collection.JavaConversions._

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
    val info = swagger.getInfo() // TODO
    val applicationKey = UrlKey.generate(info.getTitle())
    val specModelsAndEnums = parseDefinitions(swagger)
    val specModels = specModelsAndEnums._1
    val specEnums = specModelsAndEnums._2
    val resolver = Resolver(models = specModels, enums = specEnums)
    val resourcesAndParamEnums = parseResources(swagger, resolver)

    Service(
      apidoc = Apidoc(version = io.apibuilder.apidoc.spec.v0.Constants.Version),
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
      enums = (specEnums ++ resourcesAndParamEnums._2).toSet.toSeq,
      unions = Nil,
      models = specModels,
      imports = Nil,
      headers = Nil,
      resources = translators.Resource.mergeAll(resourcesAndParamEnums._1),
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
        ).flatten
    )
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
        selector.remaining.toList match {
          case Nil => (resolver.models, resolver.enums)
          case remaining => sys.error("Failed to resolve definitions: " + selector.remaining.map(_.name).mkString(", "))
        }
      }

      case Some(mydefinition) => {
        var newEnums = Seq[Enum]()
        val name = mydefinition.name
        val newModelOpt = mydefinition.definition match {
          case m: ComposedModel => {
            var composedModel: Option[Model] = None

            m.getAllOf.foreach { swaggerModel =>
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

            if(!composedModel.isDefined)
              sys.error(s"Empty composed model: $name")
            composedModel
          }

          case rm: RefModel => Some(resolver.resolveWithError(rm))
          case m: ModelImpl => {
            val translated = translators.Model(resolver, name, m)
            newEnums ++= translated._2
            translated._1
          }
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

  private def parseResources(
    swagger: Swagger,
    resolver: Resolver
  ): (Seq[Resource], Seq[Enum]) = {
    val resourceAndParamEnums = (for {
      (url, p)  <- swagger.getPaths
      operation <- p.getOperations
      response  <- operation.getResponses.toMap.get("200")
    } yield {
      val model = response.getSchema match {
        case array: ArrayProperty =>
          array.getItems
        case ref: RefProperty =>
          ref
      }

      val paramStringEnums =
        model match {
          case ref: RefProperty =>
            //Search for param enums among all operations (even the ones with no 200 response) of this resource
            for{
              resourceOp  <- p.getOperations
              param       <- resourceOp.getParameters
              if(Util.hasStringEnum(param))
            } yield {
              val httpMethod = Util.retrieveMethod(resourceOp, p).get
              val enumTypeName = Util.buildParamEnumTypeName(ref.getSimpleRef, param, httpMethod.toString)
              Enum(
                name = enumTypeName,
                plural = Text.pluralize(enumTypeName),
                description = None,
                deprecation = None,
                values = param.asInstanceOf[AbstractSerializableParameter[_]].getEnum.map { value =>
                  EnumValue(name = value, description = None, deprecation = None, attributes = Seq())
                },
                attributes = Seq())
            }
          case _ => Seq()
        }

      val resource = model match {
        case ref: RefProperty =>
          resolver.findModelByOkResponseSchema(ref.getSimpleRef) match {
            case Some(model) => translators.Resource(resolver.copy(enums = resolver.enums ++ paramStringEnums), model, url, p)
            case None => sys.error(s"Could not find model at url[$url]")
          }
      }

      (resource, paramStringEnums)
    }) toSeq

    val allResources = resourceAndParamEnums.map(_._1)
    val allParamEnums =
      (resourceAndParamEnums.flatten { case (_, seqEnums) => seqEnums })
      .toSet.toSeq  //remove duplicates (in case same param with same enum values is on several paths/operations with same method for same resource)

    (allResources, allParamEnums)
  }

}

