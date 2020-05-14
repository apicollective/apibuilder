package builder.api_json

import builder.JsonUtil
import core.{Importer, ServiceFetcher, Util, VersionMigration}
import lib.{ServiceConfiguration, ServiceValidator, UrlKey}
import io.apibuilder.spec.v0.models.Service
import play.api.libs.json.{JsObject, Json}
import com.fasterxml.jackson.core.{JsonParseException, JsonProcessingException}

import scala.util.{Failure, Success, Try}

case class ApiJsonServiceValidator(
  config: ServiceConfiguration,
  apiJson: String,
  fetcher: ServiceFetcher,
  migration: VersionMigration
) extends ServiceValidator[Service] {

  private lazy val service: Service = ServiceBuilder(migration = migration).apply(config, internalService.get)

  def validate(): Either[Seq[String], Service] = {
    errors match {
      case Nil => Right(service)
      case _ => Left(errors)
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

  private lazy val errors: Seq[String] = {
    internalService match {

      case None => {
        if (apiJson.trim == "") {
          Seq("No Data")
        } else {
          Seq(parseError.getOrElse("Invalid JSON"))
        }
      }

      case Some(_: InternalServiceForm) => {
        validateStructure() match {
          case Nil => {
            validateInfo ++
            validateKey ++
            validateImports ++
            validateAttributes("Service", internalService.get.attributes) ++
            validateHeaders ++
            validateResources ++
            validateOperations ++
            validateParameterBodies ++
            validateParameters ++
            validateResponses ++
            validateInterfaces ++
            validateUnions ++
            validateModels ++
            validateFields ++
            validateEnums ++
            validateAnnotations
          }

          case errs => {
            errs
          }
        }
      }
    }
  }

  private def validateInfo(): Seq[String] = {
    internalService.get.info match {
      case None => Nil
      case Some(info) => {
        info.license match {
          case None => Nil
          case Some(l) => {
            l.name match {
              case None => Seq("License must have a name")
              case Some(_) => Nil
            }
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
    JsonUtil.validate(
      internalService.get.json,
      strings = Seq("name"),
      optionalStrings = Seq("base_url", "description", "namespace"),
      optionalArraysOfObjects = Seq("imports", "headers", "attributes"),
      optionalObjects = Seq("apidoc", "info", "enums", "interfaces", "models", "unions", "resources", "annotations")
    )
  }

  private def validateImports(): Seq[String] = {
    internalService.get.imports.flatMap(_.warnings) ++
    internalService.get.imports.flatMap { imp =>
      imp.uri match {
        case None => None
        case Some(uri) => {
          Util.validateUri(uri) match {
            case Nil => Importer(fetcher, uri).validate  // TODO. need to cache somewhere to avoid a second lookup when parsing later
            case errs => errs
          }
        }
      }
    }
  }

  private def validateUnions(): Seq[String] = {
    val warnings = internalService.get.unions.flatMap(_.warnings)

    val attributeErrors = internalService.get.unions.flatMap { union =>
      validateAttributes(s"Union[${union.name}]", union.attributes)
    }

    val typeErrors = internalService.get.unions.flatMap { union =>
      validateUnionTypes(union)
    }

    warnings ++ attributeErrors ++ typeErrors
  }

  private def validateUnionTypes(union: InternalUnionForm): Seq[String] = {
    val attributeErrors = union.types.filter(_.datatype.isRight).flatMap { typ =>
      validateAttributes(s"Union[${union.name}] type[${typ.datatype.right.get}]", typ.attributes)
    }
    val typeErrors = union.types.filter(_.datatype.isLeft).flatMap { typ =>
      Seq(s"Union[${union.name}] type[] " + typ.datatype.left.get.mkString(", "))
    }
    union.types.flatMap(_.warnings) ++ attributeErrors ++ typeErrors
  }

  private def validateAnnotations(): Seq[String] = {
    val warnings = internalService.get.annotations.flatMap(_.warnings)

    val missingNames = internalService.get.annotations.filter(_.name.isEmpty).map(_=>
      "Annotations must have a name"
    )

    warnings ++ missingNames
  }

  private def validateEnums(): Seq[String] = {
    val warnings = internalService.get.enums.flatMap(_.warnings)

    val attributeErrors = internalService.get.enums.flatMap { enum =>
      validateAttributes(s"Enum[${enum.name}]", enum.attributes)
    }

    val valueErrors = internalService.get.enums.flatMap { enum =>
      validateEnumValues(enum)
    }

    warnings ++ attributeErrors ++ valueErrors
  }

  private def validateEnumValues(enum: InternalEnumForm): Seq[String] = {
    val attributeErrors = enum.values.zipWithIndex.flatMap { case (value, i) =>
      value.name match {
        case None => Seq(s"Enum[${enum.name}] value[$i]: Missing name")
        case Some(name) => validateAttributes(s"Enum[${enum.name}] value[$name]", value.attributes)
      }
    }

    enum.values.flatMap(_.warnings) ++ attributeErrors
  }

  private def validateInterfaces(): Seq[String] = {
    val warnings = internalService.get.interfaces.flatMap(_.warnings)

    val attributeErrors = internalService.get.interfaces.flatMap { interface =>
      validateAttributes(s"Interface[${interface.name}]", interface.attributes)
    }

    warnings ++ attributeErrors
  }

  private def validateModels(): Seq[String] = {
    val models = internalService.get.models
    val warnings = models.flatMap(_.warnings)

    val attributeErrors = models.flatMap { model =>
      validateAttributes(s"Model[${model.name}]", model.attributes)
    }

    warnings ++ attributeErrors
  }

  private def validateHeaders(): Seq[String] = {
    val warnings = internalService.get.headers.flatMap(_.warnings)

    val attributeErrors = internalService.get.headers.filter(_.name.isDefined).flatMap { header =>
      validateAttributes(s"Header[${header.name}]", header.attributes)
    }

    warnings ++ attributeErrors
  }

  private def validateFields(): Seq[String] = {
    val missingNames = internalService.get.models.flatMap { model =>
      model.fields.filter(_.name.isEmpty).map { f =>
        s"Model[${model.name}] field[${f.name}] must have a name"
      }
    }

    val missingTypes = internalService.get.models.flatMap { model =>
      model.fields.filter(_.name.isDefined).filter(_.datatype.isLeft).map { f =>
        s"Model[${model.name}] field[${f.name.get}] type ${f.datatype.left.get.mkString(", ")}"
      }
    }

    val attributeErrors = internalService.get.models.flatMap { model =>
      model.fields.filter(_.name.isDefined).flatMap { f =>
        validateAttributes(s"Model[${model.name}] field[${f.name.get}]", f.attributes)
      }
    }

    val warnings = internalService.get.models.flatMap { model =>
      model.fields.filter(f => f.warnings.nonEmpty && f.name.isDefined).map { f =>
        s"Model[${model.name}] field[${f.name.get}]: " + f.warnings.mkString(", ")
      }
    }

    missingTypes ++ missingNames ++ attributeErrors ++ warnings
  }

  private def validateAttributes(prefix: String, attributes: Seq[InternalAttributeForm]): Seq[String] = {
    attributes.zipWithIndex.flatMap { case (attr, i) =>
      val fieldErrors = attr.name match {
        case None => Seq(s"$prefix: Attribute $i must have a name")
        case Some(name) => {
          attr.value match {
            case None => Seq(s"$prefix: Attribute $name must have a value")
            case Some(_) => Nil
          }
        }
      }

      fieldErrors ++ attr.warnings.map { err => s"$prefix: $err" }
    }
  }

  private def validateResponses(): Seq[String] = {
    val codeErrors = internalService.get.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.responses.filter(r => r.warnings.nonEmpty).map { r =>
          opLabel(resource, op, s"${r.code}: " + r.warnings.mkString(", "))
        }
      }
    }

    val typeErrors = internalService.get.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.responses.filter(r => r.datatype.isLeft).map { r =>
          opLabel(resource, op, s"${r.code} type: " + r.datatype.left.get.mkString(", "))
        }
      }
    }

    val attributeErrors = internalService.get.resources.flatMap { resource =>
      validateAttributes(s"Resource[${resource.datatype.label}]", resource.attributes)
    }

    codeErrors ++ typeErrors ++ attributeErrors
  }

  private def validateParameterBodies(): Seq[String] = {
    internalService.get.resources.flatMap { resource =>
      resource.operations.filter(_.body.isDefined).flatMap { op =>
        op.body.get.datatype match {
          case Left(errs) => Some(opLabel(resource, op, s"Body ${errs.mkString(", ")}"))
          case Right(_) => None
        }
      }
    }
  }

  private def validateResources(): Seq[String] = {
    internalService.get.resources.filter(_.warnings.nonEmpty).map { resource =>
      s"Resource[${resource.datatype.label}] " + resource.warnings.mkString(", ")
    }
  }

  private def validateOperations(): Seq[String] = {
    val warnings = internalService.get.resources.flatMap { resource =>
      resource.operations.filter(_.warnings.nonEmpty).map { op =>
        opLabel(resource, op, op.warnings.mkString(", "))
      }
    }

    val attributeErrors = internalService.get.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        validateAttributes(opLabel(resource, op, ""), op.attributes)
      }
    }

    val bodyAttributeErrors = internalService.get.resources.flatMap { resource =>
      resource.operations.filter(_.body.isDefined).flatMap { op =>
        validateAttributes(opLabel(resource, op, "body"), op.body.get.attributes)
      }
    }

    warnings ++ attributeErrors ++ bodyAttributeErrors
  }

  private def validateParameters(): Seq[String] = {
    internalService.get.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.parameters.flatMap(_.warnings)
      }
    }
  }

  private def opLabel(
    resource: InternalResourceForm,
    op: InternalOperationForm,
    message: String
  ): String = {
    Seq(
      s"Resource[${resource.datatype.label}]",
      op.method.getOrElse(""),
      Seq(
        resource.path,
        op.path
      ).flatten.mkString(""),
      message
    ).map(_.trim).filter(!_.isEmpty).mkString(" ")
  }

}
