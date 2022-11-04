package builder.api_json

import builder.JsonUtil
import core.{DuplicateJsonParser, Importer, ServiceFetcher, Util, VersionMigration}
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
            validateInfo() ++
            validateKey() ++
            validateImports() ++
            validateAttributes("Service", internalService.get.attributes) ++
            validateHeaders() ++
            validateResources(internalService.get.resources, prefix = None) ++
            validateInterfaces() ++
            validateTemplates() ++
            validateUnions() ++
            validateModels(internalService.get.models, prefix = None) ++
            validateEnums() ++
            validateAnnotations() ++
            DuplicateJsonParser.validateDuplicates(apiJson)
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
      optionalStrings = Seq("base_url", "description", "namespace", "$schema"),
      optionalArraysOfObjects = Seq("imports", "headers", "attributes"),
      optionalObjects = Seq("info", "enums", "interfaces", "models", "unions", "resources", "annotations", "templates")
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
    val attributeErrors = union.types.flatMap { typ =>
      typ.datatype match {
        case Left(_) => None
        case Right(dt) => validateAttributes(s"Union[${union.name}] type[${dt.name}]", typ.attributes)
      }
    }
    val typeErrors = union.types.flatMap { typ =>
      typ.datatype match {
        case Left(errors) => Seq(s"Union[${union.name}] type[] " + errors.mkString(", "))
        case Right(_) => Nil
      }
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

    val valueErrors = internalService.get.enums.flatMap(validateEnumValues)

    warnings ++ attributeErrors ++ valueErrors
  }

  private def validateEnumValues(`enum`: InternalEnumForm): Seq[String] = {
    val attributeErrors = `enum`.values.zipWithIndex.flatMap { case (value, i) =>
      value.name match {
        case None => Seq(s"Enum[${`enum`.name}] value[$i]: Missing name")
        case Some(name) => validateAttributes(s"Enum[${`enum`.name}] value[$name]", value.attributes)
      }
    }

    `enum`.values.flatMap(_.warnings) ++ attributeErrors
  }

  private def validateInterfaces(): Seq[String] = {
    val warnings = internalService.get.interfaces.flatMap(_.warnings)

    val attributeErrors = internalService.get.interfaces.flatMap { interface =>
      validateAttributes(s"Interface[${interface.name}]", interface.attributes)
    }

    warnings ++ attributeErrors
  }

  private def validateTemplates(): Seq[String] = {
    val templates = internalService.get.templates
    val warnings = templates.models.filter(_.warnings.nonEmpty).map { m =>
      modelLabel(m, m.warnings.mkString(", "), prefix = Some("Template"))
    } ++ templates.resources.filter(_.warnings.nonEmpty).map { r =>
      resourceLabel(r, r.warnings.mkString(", "), prefix = Some("Template"))
    }

    warnings ++ validateModels(templates.models, prefix = Some("Template")) ++
      validateResources(templates.resources, prefix = Some("Template")) ++
      validateTemplateModelNames()
  }

  private[this] def validateTemplateModelNames(): Seq[String] = {
    val templateNames = internalService.get.templates.models.map(_.name)
    val interfaceDups = internalService.get.interfaces.filter { i => templateNames.contains(i.name) }.map { i =>
      s"Name[${i.name}] cannot be used as the name of both an interface and a template model"
    }
    val modelDups = internalService.get.models.filter { m => templateNames.contains(m.name) }.map { m =>
      s"Name[${m.name}] cannot be used as the name of both a model and a template model"
    }
    interfaceDups ++ modelDups
  }

  private def validateModels(models: Seq[InternalModelForm], prefix: Option[String]): Seq[String] = {
    val warnings = models.flatMap(_.warnings)

    val attributeErrors = models.flatMap { model =>
      validateAttributes(labelWitPrefix(s"Model[${model.name}]", prefix), model.attributes)
    }

    warnings ++ attributeErrors ++ validateFields(models, prefix)
  }

  private[this] def labelWitPrefix(base: String, prefix: Option[String]): String = {
    prefix match {
      case None => base
      case Some(p) => s"$p $base"
    }
  }

  private def validateHeaders(): Seq[String] = {
    val warnings = internalService.get.headers.flatMap(_.warnings)

    val attributeErrors = internalService.get.headers.filter(_.name.isDefined).flatMap { header =>
      validateAttributes(s"Header[${header.name}]", header.attributes)
    }

    warnings ++ attributeErrors
  }

  private def validateFields(models: Seq[InternalModelForm], prefix: Option[String]): Seq[String] = {
    val missingNames = models.flatMap { model =>
      model.fields.filter(_.name.isEmpty).map { f =>
        modelLabel(model, s"field[${f.name}] must have a name", prefix = prefix)
      }
    }

    val missingTypes = models.flatMap { model =>
      model.fields.filter(_.name.isDefined).flatMap { f =>
        f.datatype match {
          case Left(errors) => Some({
            modelLabel(model, s"field[${f.name.get}] type ${errors.mkString(", ")}", prefix = prefix)
          })
          case Right(_) => None
        }
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

  private def validateResponses(resources: Seq[InternalResourceForm], prefix: Option[String]): Seq[String] = {
    val codeErrors = resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.declaredResponses.filter(r => r.warnings.nonEmpty).map { r =>
          opLabel(resource, op, s"${r.code}: " + r.warnings.mkString(", "), prefix = prefix)
        }
      }
    }

    val typeErrors = resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.declaredResponses.flatMap { r =>
          r.datatype match {
            case Left(errors) => Some(opLabel(
              resource, op, s"${r.code} type: " + errors.mkString(", "), prefix = prefix
            ))
            case Right(_) => None
          }
        }
      }
    }

    val attributeErrors = internalService.get.resources.flatMap { resource =>
      validateAttributes(s"Resource[${resource.datatype.label}]", resource.attributes)
    }

    codeErrors ++ typeErrors ++ attributeErrors
  }

  private def validateParameterBodies(resources: Seq[InternalResourceForm], prefix: Option[String]): Seq[String] = {
    resources.flatMap { resource =>
      resource.operations.filter(_.body.isDefined).flatMap { op =>
        op.body.get.datatype match {
          case Left(errs) => Some(opLabel(resource, op, s"Body ${errs.mkString(", ")}", prefix = prefix))
          case Right(_) => None
        }
      }
    }
  }

  private def validateResources(resources: Seq[InternalResourceForm], prefix: Option[String]): Seq[String] = {
    val resourceWarnings = resources.filter(_.warnings.nonEmpty).map { resource =>
      labelWitPrefix(s"Resource[${resource.datatype.label}]", prefix) + resource.warnings.mkString(", ")
    }
    resourceWarnings ++
      validateOperations(resources, prefix) ++
      validateParameterBodies(resources, prefix) ++
      validateParameters(resources, prefix) ++
      validateResponses(resources, prefix)
  }

  private def validateOperations(resources: Seq[InternalResourceForm], prefix: Option[String]): Seq[String] = {
    val warnings = resources.flatMap { resource =>
      resource.operations.filter(_.warnings.nonEmpty).map { op =>
        opLabel(resource, op, op.warnings.mkString(", "), prefix = prefix)
      }
    }

    val attributeErrors = internalService.get.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        validateAttributes(opLabel(resource, op, "", prefix = prefix), op.attributes)
      }
    }

    val bodyAttributeErrors = internalService.get.resources.flatMap { resource =>
      resource.operations.filter(_.body.isDefined).flatMap { op =>
        validateAttributes(opLabel(resource, op, "body", prefix = prefix), op.body.get.attributes)
      }
    }

    warnings ++ attributeErrors ++ bodyAttributeErrors
  }

  private def validateParameters(resources: Seq[InternalResourceForm], prefix: Option[String]): Seq[String] = {
    resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.parameters.filter(_.warnings.nonEmpty).map { p =>
          opLabel(resource, op, s"Parameter[${p.name}]", prefix = prefix)
        }
      }
    }
  }

  private def resourceLabel(
    resource: InternalResourceForm,
    message: String,
    prefix: Option[String]
  ): String = {
    labelWitPrefix(s"Resource[${resource.datatype.label}] $message", prefix)
  }

  private def opLabel(
    resource: InternalResourceForm,
    op: InternalOperationForm,
    message: String,
    prefix: Option[String]
  ): String = {
    Seq(
      labelWitPrefix(s"Resource[${resource.datatype.label}]", prefix),
      op.method.getOrElse(""),
      Seq(
        resource.path,
        op.path
      ).flatten.mkString(""),
      message
    ).map(_.trim).filter(_.nonEmpty).mkString(" ")
  }

  private def modelLabel(model: InternalModelForm, message: String, prefix: Option[String]): String = {
    labelWitPrefix(s"Model[${model.name}] $message", prefix)
  }
}
