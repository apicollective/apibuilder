package builder.api_json

import builder.JsonUtil
import builder.api_json.templates.JsMerge
import cats.implicits._
import cats.data.ValidatedNec
import cats.data.Validated.{Invalid, Valid}
import core.{DuplicateJsonParser, Importer, ServiceFetcher, Util, VersionMigration}
import lib.{ServiceConfiguration, ServiceValidator, UrlKey, ValidatedHelpers}
import io.apibuilder.spec.v0.models.Service
import play.api.libs.json.{JsObject, JsValue, Json}
import com.fasterxml.jackson.core.{JsonParseException, JsonProcessingException}

import scala.util.{Failure, Success, Try}

case class ApiJsonServiceValidator(
  config: ServiceConfiguration,
  fetcher: ServiceFetcher,
  migration: VersionMigration
) extends ServiceValidator[Service] with ValidatedHelpers {

  override def validate(rawJson: String): ValidatedNec[String, Service] = {
    parseRawJson(rawJson).andThen { form =>
      sequenceUnique(Seq(
        validateStructure(form.json),
        validateInternalApiJsonForm(rawJson, form)
      )).map { _ =>
        ServiceBuilder(migration = migration).apply(config, form)
      }
    }
  }

  private def parseRawJson(rawJson: String): ValidatedNec[String, InternalApiJsonForm] = {
    if (rawJson.trim == "") {
      "No Data".invalidNec
    } else {
      Try(Json.parse(rawJson)) match {
        case Success(v) => {
          v.asOpt[JsObject] match {
            case Some(o) => JsMerge.merge(o).map { js =>
              InternalApiJsonForm(js, fetcher)
            }
            case None => "Must upload a Json Object".invalidNec
          }
        }
        case Failure(ex) => ex match {
          case e: JsonParseException => e.getMessage.invalidNec
          case e: JsonProcessingException => e.getMessage.invalidNec
          case _ => "Unknown error parsing JSON".invalidNec
        }
      }
    }
  }

  private def validateInfo(info: Option[InternalInfoForm]): ValidatedNec[String, Unit] = {
    info match {
      case None => ().validNec
      case Some(info) => {
        info.license match {
          case None => ().validNec
          case Some(l) => {
            l.name match {
              case None => "License must have a name".invalidNec
              case Some(_) => ().validNec
            }
          }
        }
      }
    }
  }

  private def validateKey(key: Option[String]): ValidatedNec[String, Unit] = {
    key match {
      case None => ().validNec
      case Some(key) => {
        val generated = UrlKey.generate(key)
        if (generated == key) {
          ().validNec
        } else {
          s"Invalid url key. A valid key would be $generated".invalidNec
        }
      }
    }
  }

  private def validateInternalApiJsonForm(rawInput: String, form: InternalApiJsonForm): ValidatedNec[String, Unit] = {
    sequenceUnique(Seq(
      validateInfo(form.info),
      validateKey(form.key),
      validateImports(form.imports),
      validateAttributes("Service", form.attributes),
      validateHeaders(form.headers),
      validateResources(form.resources),
      validateInterfaces(form.interfaces),
      validateUnions(form),
      validateModels(form),
      validateEnums(form.enums),
      validateAnnotations(form.annotations),
      DuplicateJsonParser.validateDuplicates(rawInput)
    ))
  }
  private def validateStructure(json: JsValue): ValidatedNec[String, Unit] = {
    JsonUtil.validate(
      json,
      strings = Seq("name"),
      optionalStrings = Seq("base_url", "description", "namespace", "$schema"),
      optionalArraysOfObjects = Seq("imports", "headers", "attributes"),
      optionalObjects = Seq("info", "enums", "interfaces", "models", "unions", "resources", "annotations", "templates")
    )
  }

  private def validateImports(imports: Seq[InternalImportForm]): ValidatedNec[String, Unit] = {
    sequenceUnique(
      imports.map(_.warnings) ++
      imports.flatMap { imp =>
        imp.uri match {
          case None => Seq(().validNec)
          case Some(uri) => {
            Util.validateUri(uri) match {
              case Nil => Seq(Importer(fetcher, uri).validate)  // TODO. need to cache somewhere to avoid a second lookup when parsing later
              case errs => errs.map(_.invalidNec)
            }
          }
        }
      }
    )
  }

  private def validateUnions(form: InternalApiJsonForm): ValidatedNec[String, Unit] = {
    sequenceUnique(
      form.unions.map(_.warnings) ++ form.unions.map { union =>
        (validateAttributes(s"Union[${union.name}]", union.attributes),
          validateTypeInterfaces(form, s"Union[${union.name}]", union.interfaces)
        ).mapN { case (_, _) => () }
      } ++ form.unions.map(validateUnionTypes)
    )
  }

  private def validateUnionTypes(union: InternalUnionForm): ValidatedNec[String, Unit] = {
    sequenceUnique(
      union.types.map(_.warnings) ++ union.types.map { typ =>
        typ.datatype match {
          case Invalid(errors) => (s"Union[${union.name}] type[] " + errors.toNonEmptyList.toList.mkString(", ")).invalidNec
          case Valid(dt) => validateAttributes(s"Union[${union.name}] type[${dt.name}]", typ.attributes)
        }
      }
    )
  }

  private def validateAnnotations(annotations: Seq[InternalAnnotationForm]): ValidatedNec[String, Unit] = {
    sequenceUnique(
      annotations.map(_.warnings) ++ annotations.filter(_.name.isEmpty).map(_ =>
        "Annotations must have a name".invalidNec
      )
    )
  }

  private def validateEnums(enums: Seq[InternalEnumForm]): ValidatedNec[String, Unit] = {
    sequenceUnique(
      enums.map(_.warnings) ++ enums.map { e =>
        validateAttributes(s"Enum[${e.name}]", e.attributes)
      } ++ enums.map(validateEnumValues)
    )
  }

  private def validateEnumValues(`enum`: InternalEnumForm): ValidatedNec[String, Unit] = {
    sequenceUnique(
      `enum`.values.map(_.warnings) ++ `enum`.values.zipWithIndex.map { case (value, i) =>
        value.name match {
          case None => s"Enum[${`enum`.name}] value[$i]: Missing name".invalidNec
          case Some(name) => validateAttributes(s"Enum[${`enum`.name}] value[$name]", value.attributes)
        }
      }
    )
  }

  private def validateInterfaces(interfaces: Seq[InternalInterfaceForm]): ValidatedNec[String, Unit] = {
    sequenceUnique(
      interfaces.map(_.warnings) ++ interfaces.map { interface =>
        validateAttributes(s"Interface[${interface.name}]", interface.attributes)
      }
    )
  }

  private def validateModels(form: InternalApiJsonForm): ValidatedNec[String, Unit] = {
    sequenceUnique(
      form.models.map(_.warnings) ++ form.models.map { model =>
        (validateAttributes(s"Model[${model.name}]", model.attributes),
          validateTypeInterfaces(form, s"Model[${model.name}]", model.interfaces)
        ).mapN { case (_, _) => () }
      } ++ Seq(validateFields(form.models))
    )
  }

  private def validateTypeInterfaces(form: InternalApiJsonForm, name: String, interfaces: Seq[String]): ValidatedNec[String, Unit] = {
    interfaces.map { iName =>
      if (form.interfaces.exists(_.name == iName)) {
        ().validNec
      } else {
        s"$name cannot find interface named '$iName'".invalidNec
      }
    }.sequence.map { _ => () }
  }

  private def validateHeaders(headers: Seq[InternalHeaderForm]): ValidatedNec[String, Unit] = {
    sequenceUnique(
      headers.map(_.warnings) ++ headers.filter(_.name.isDefined).map { header =>
        validateAttributes(s"Header[${header.name}]", header.attributes)
      }
    )
  }

  private def validateFields(models: Seq[InternalModelForm]): ValidatedNec[String, Unit] = {
    val missingNames = models.flatMap { model =>
      model.fields.filter(_.name.isEmpty).zipWithIndex.map { case (_, i) =>
        modelLabel(model, s"field[$i] must have a name").invalidNec
      }
    }

    val missingTypes = models.flatMap { model =>
      model.fields.filter(_.name.isDefined).map { f =>
        f.datatype match {
          case Invalid(errors) => {
            modelLabel(model, s"field[${f.name.get}] type ${errors.toNonEmptyList.toList.mkString(", ")}").invalidNec
          }
          case Valid(_) => ().validNec
        }
      }
    }

    val attributeErrors = models.flatMap { model =>
      model.fields.filter(_.name.isDefined).map { f =>
        validateAttributes(s"Model[${model.name}] field[${f.name.get}]", f.attributes)
      }
    }

    val warnings = models.flatMap { model =>
      model.fields.filter(f => f.warnings.isInvalid && f.name.isDefined).map { f =>
        (s"Model[${model.name}] field[${f.name.get}]: " + formatErrors(f.warnings)).invalidNec
      }
    }

    sequenceUnique(missingTypes ++ missingNames ++ attributeErrors ++ warnings)
  }

  private def validateAttributes(prefix: String, attributes: Seq[InternalAttributeForm]): ValidatedNec[String, Unit] = {
    sequenceUnique(
      attributes.zipWithIndex.map { case (attr, i) =>
        val fieldErrors = attr.name match {
          case None => s"$prefix: Attribute $i must have a name".invalidNec
          case Some(name) => {
            attr.value match {
              case None => s"$prefix: Attribute $name must have a value".invalidNec
              case Some(_) => ().validNec
            }
          }
        }

        sequenceUnique(Seq(fieldErrors, addPrefixToError(prefix, attr.warnings)))
      }
    )
  }

  private def validateResponses(resources: Seq[InternalResourceForm]): ValidatedNec[String, Unit] = {
    val codeErrors = resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.declaredResponses.filter(r => r.warnings.isInvalid).map { r =>
          opLabel(resource, op, s"${r.code}: " + formatErrors(r.warnings)).invalidNec
        }
      }
    }

    val typeErrors = resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.declaredResponses.map { r =>
          r.datatype match {
            case Invalid(errors) => opLabel(resource, op, s"${r.code} type: " + errors.toNonEmptyList.toList.mkString(", ")).invalidNec
            case Valid(_) => ().validNec
          }
        }
      }
    }

    val attributeErrors = resources.map { resource =>
      validateAttributes(s"Resource[${resource.datatype.label}]", resource.attributes)
    }

    sequenceUnique(codeErrors ++ typeErrors ++ attributeErrors)
  }

  private def validateResourceBodies(resources: Seq[InternalResourceForm]): ValidatedNec[String, Unit] = {
    sequenceUnique(
      resources.flatMap { resource =>
        resource.operations.filter(_.body.isDefined).map { op =>
          op.body.get.datatype match {
            case Invalid(errs) => opLabel(resource, op, s"Body ${errs.toNonEmptyList.toList.mkString(", ")}").invalidNec
            case Valid(_) => ().validNec
          }
        }
      }
    )
  }

  private def validateResources(resources: Seq[InternalResourceForm]): ValidatedNec[String, Unit] = {
    val resourceWarnings = sequenceUnique(resources.filter(_.warnings.isInvalid).map { resource =>
      (s"Resource[${resource.datatype.label}]" + formatErrors(resource.warnings)).invalidNec
    })

    sequenceUnique(Seq(
      resourceWarnings,
      validateOperations(resources),
      validateResourceBodies(resources),
      validateParameters(resources),
      validateResponses(resources)
    ))
  }

  private def validateOperations(resources: Seq[InternalResourceForm]): ValidatedNec[String, Unit] = {
    val warnings = resources.flatMap { resource =>
      resource.operations.filter(_.warnings.isInvalid).map { op =>
        opLabel(resource, op, formatErrors(op.warnings)).invalidNec
      }
    }

    val attributeErrors = resources.flatMap { resource =>
      resource.operations.map { op =>
        validateAttributes(opLabel(resource, op, ""), op.attributes)
      }
    }

    val bodyAttributeErrors = resources.flatMap { resource =>
      resource.operations.filter(_.body.isDefined).map { op =>
        validateAttributes(opLabel(resource, op, "body"), op.body.get.attributes)
      }
    }

    sequenceUnique(warnings ++ attributeErrors ++ bodyAttributeErrors)
  }

  private def validateParameters(resources: Seq[InternalResourceForm]): ValidatedNec[String, Unit] = {
    sequenceUnique(
      resources.flatMap { resource =>
        resource.operations.flatMap { op =>
          op.parameters.filter(_.warnings.isInvalid).filter(_.name.nonEmpty).map { p =>
            opLabel(resource, op, s"Parameter[${p.name.get}] ${formatErrors(p.warnings)}").invalidNec
          }
        }
      }
    )
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
    ).map(_.trim).filter(_.nonEmpty).mkString(" ")
  }

  private def modelLabel(model: InternalModelForm, message: String): String = {
    s"Model[${model.name}] $message"
  }
}
