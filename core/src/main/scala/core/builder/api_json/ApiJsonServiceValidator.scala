package builder.api_json

import builder.{JsonUtil, ServiceValidator}
import core.{ClientFetcher, Importer, ServiceConfiguration, ServiceFetcher, Util}
import lib.UrlKey
import com.gilt.apidoc.spec.v0.models.Service
import play.api.libs.json.{Json, JsObject}
import com.fasterxml.jackson.core.{ JsonParseException, JsonProcessingException }
import scala.util.{Failure, Success, Try}

case class ApiJsonServiceValidator(
  config: ServiceConfiguration,
  apiJson: String,
  fetcher: ServiceFetcher = new ClientFetcher()
) extends ServiceValidator {

  lazy val service: Service = ServiceBuilder(config, internalService.get)

  def validate(): Either[Seq[String], Service] = {
    if (isValid) {
      Right(service)
    } else {
      Left(errors)
    }
  }

  private var parseError: Option[String] = None

  lazy val serviceForm: Option[JsObject] = {
    Try(Json.parse(apiJson)) match {
      case Success(v) => {
        v.asOpt[JsObject] match {
          case Some(o) => {
            Some(o)
          }
          case None => {
            parseError = Some("Must upload a Json Object")
            None
          }
        }
      }
      case Failure(ex) => ex match {
        case e: JsonParseException => {
          parseError = Some(e.getMessage)
          None
        }
        case e: JsonProcessingException => {
          parseError = Some(e.getMessage)
          None
        }
      }
    }
  }

  private lazy val internalService: Option[InternalServiceForm] = serviceForm.map(InternalServiceForm(_, fetcher))

  lazy val errors: Seq[String] = internalErrors match {
    case Nil => builder.ServiceSpecValidator(service).errors
    case e => e
  }

  private lazy val internalErrors: Seq[String] = {
    internalService match {

      case None => {
        if (apiJson.trim == "") {
          Seq("No Data")
        } else {
          Seq(parseError.getOrElse("Invalid JSON"))
        }
      }

      case Some(sd: InternalServiceForm) => {
        validateStructure match {
          case Nil => {
            validateKey ++
            validateImports ++
            validateEnums ++
            validateUnions ++
            validateHeaders ++
            validateFields ++
            validateResources ++
            validateOperations ++
            validateParameterBodies ++
            validateParameters ++
            validateResponses
          }
          case errors => {
            errors
          }
        }
      }
    }
  }

  private def validateKey(): Seq[String] = {
    internalService.get.key match {
      case None => Seq.empty
      case Some(key) => {
        val generated = UrlKey.generate(key)
        if (generated == key) {
          Seq.empty
        } else {
          Seq(s"Invalid url key. A valid key would be $generated")
        }
      }
    }
  }

  private def validateStructure(): Seq[String] = {
    val invalid = JsonUtil.validate(
      internalService.get.json,
      strings = Seq("name"),
      optionalStrings = Seq("base_url", "description"),
      optionalArraysOfObjects = Seq("imports", "headers"),
      optionalObjects = Seq("enums", "models", "unions", "resources")
    )

    val models: Seq[String] = (internalService.get.json \ "models").asOpt[JsObject] match {
      case None => Seq.empty
      case Some(model) => {
        model.value.flatMap { case (name, js) =>
          JsonUtil.validate(
            js,
            optionalStrings = Seq("description", "plural"),
            arraysOfObjects = Seq("fields"),
            prefix = Some(s"Model[$name]")
          )
        }.toSeq
      }
    }

    val enums: Seq[String] = (internalService.get.json \ "enums").asOpt[JsObject] match {
      case None => Seq.empty
      case Some(enum) => {
        enum.value.flatMap { case (name, js) =>
          JsonUtil.validate(
            js,
            optionalStrings = Seq("description", "plural"),
            arraysOfObjects = Seq("values"),
            prefix = Some(s"Enum[$name]")
          )
        }.toSeq
      }
    }

    val unions: Seq[String] = (internalService.get.json \ "unions").asOpt[JsObject] match {
      case None => Seq.empty
      case Some(js) => {
        js.value.flatMap { case (name, js) =>
          JsonUtil.validate(
            js,
            optionalStrings = Seq("description", "plural"),
            arraysOfObjects = Seq("types"),
            prefix = Some(s"Union[$name]")
          )
        }.toSeq
      }
    }

    invalid ++ models ++ enums ++ unions
  }

  private def validateImports(): Seq[String] = {
    internalService.get.imports.flatMap { imp =>
      imp.uri match {
        case None => Seq("imports.uri is required")
        case Some(uri) => {
          Util.validateUri(uri) match {
            case Nil => Importer(fetcher, uri).validate  // TODO. need to cache somewhere to avoid a second lookup when parsing later
            case errors => errors
          }
        }
      }
    }
  }

  private def validateEnums(): Seq[String] = {
    internalService.get.enums.flatMap { enum =>
      enum.values.filter(_.name.isEmpty).map { value =>
        s"Enum[${enum.name}] - all values must have a name"
      }
    }
  }

  private def validateUnions(): Seq[String] = {
    internalService.get.unions.filter { !_.types.filter(_.datatype.isEmpty).isEmpty }.map { union =>
      s"Union[${union.name}] all types must have a name"
    }
  }

  private def validateHeaders(): Seq[String] = {
    internalService.get.headers.flatMap(_.warnings)
  }

  private def validateFields(): Seq[String] = {
    val missingNames = internalService.get.models.flatMap { model =>
      model.fields.filter(_.name.isEmpty).map { f =>
        s"Model[${model.name}] field[${f.name}] must have a name"
      }
    }

    val missingTypes = internalService.get.models.flatMap { model =>
      model.fields.filter(!_.name.isEmpty).filter(_.datatype.isEmpty).map { f =>
        s"Model[${model.name}] field[${f.name.get}] must have a type"
      }
    }

    val warnings = internalService.get.models.flatMap { model =>
      model.fields.filter(f => !f.warnings.isEmpty && !f.name.isEmpty).map { f =>
        s"Model[${model.name}] field[${f.name.get}]: " + f.warnings.mkString(", ")
      }
    }

    missingTypes ++ missingNames ++ warnings
  }


  private def validateResponses(): Seq[String] = {
    val invalidCodes = internalService.get.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.responses.flatMap { r =>
          Try(r.code.toInt) match {
            case Success(v) => None
            case Failure(ex) => ex match {
              case e: java.lang.NumberFormatException => {
                Some(opLabel(resource, op, s"Response code is not an integer[${r.code}]"))
              }
            }
          }
        }
      }
    }

    val warnings = internalService.get.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.responses.filter(r => !r.warnings.isEmpty).map { r =>
          opLabel(resource, op, s"${r.code}: " + r.warnings.mkString(", "))
        }
      }
    }

    invalidCodes ++ warnings
  }

  private def validateParameterBodies(): Seq[String] = {
    internalService.get.resources.flatMap { resource =>
      resource.operations.filter(!_.body.isEmpty).flatMap { op =>
        op.body.flatMap(_.datatype) match {
          case None => Some(opLabel(resource, op, "Body missing type"))
          case Some(_) => None
        }
      }
    }
  }

  private def validateResources(): Seq[String] = {
    internalService.get.resources.filter(!_.warnings.isEmpty).map { resource =>
      s"Resource[${resource.datatype.label}] " + resource.warnings.mkString(", ")
    }
  }

  private def validateOperations(): Seq[String] = {
    internalService.get.resources.flatMap { resource =>
      resource.operations.filter(!_.warnings.isEmpty).map { op =>
        opLabel(resource, op, op.warnings.mkString(", "))
      }
    }
  }

  private def validateParameters(): Seq[String] = {
    val missingNames = internalService.get.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.parameters.filter(_.name.isEmpty).map { p =>
          opLabel(resource, op, "Missing name")
        }
        op.parameters.filter(_.datatype.isEmpty).map { p =>
          opLabel(resource, op, "Missing type")
        }
      }
    }

    missingNames
  }

  private def opLabel(
    resource: InternalResourceForm,
    op: InternalOperationForm,
    message: String
  ): String = {
    Seq(
      s"Resource[${resource.datatype.label}]",
      op.method.getOrElse("").trim,
      op.path.trim,
      message.trim
    ).filter(_ != "").mkString(" ")
  }

}
