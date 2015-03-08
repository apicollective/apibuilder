package builder

import core.Importer
import com.gilt.apidoc.spec.v0.models.Service
import lib.{Datatype, DatatypeResolver, Kind, Primitives, Text, Type}

case class ServiceSpecValidator(
  service: Service
) {

  private val typeResolver = DatatypeResolver(
    enumNames = service.enums.map(_.name) ++ service.imports.flatMap { service =>
      service.enums.map { enum =>
        s"${service.namespace}.enums.${enum}"
      }
    },

    modelNames = service.models.map(_.name) ++ service.imports.flatMap { service =>
      service.models.map { model =>
        s"${service.namespace}.models.${model}"
      }
    },

    unionNames = service.unions.map(_.name) ++ service.imports.flatMap { service =>
      service.unions.map { union =>
        s"${service.namespace}.unions.${union}"
      }
    }
  )

  lazy val errors: Seq[String] = {
    validateName() ++
    validateBaseUrl() ++
    validateModels() ++
    validateEnums() ++
    validateUnions() ++
    validateHeaders() ++
    validateModelAndEnumAndUnionNamesAreDistinct() ++
    validateFields()
  }

  private def validateName(): Seq[String] = {
    if (Text.startsWithLetter(service.name)) {
      Seq.empty
    } else {
      Seq(s"Name[${service.name}] must start with a letter")
    }
  }

  private def validateBaseUrl(): Seq[String] = {
    service.baseUrl match {
      case Some(url) => { 
        if(url.endsWith("/")){
          Seq(s"base_url[$url] must not end with a '/'")  
        } else {
          Seq.empty
        } 
      }
      case None => Seq.empty
    }
  }

  private def validateModels(): Seq[String] = {
    val nameErrors = service.models.flatMap { model =>
      Text.validateName(model.name) match {
        case Nil => None
        case errors => {
          Some(s"Model[${model.name}] name is invalid: ${errors.mkString(" ")}")
        }
      }
    }

    val fieldErrors = service.models.filter { _.fields.isEmpty }.map { model =>
      s"Model[${model.name}] must have at least one field"
    }

    val duplicates = dupsError("Model", service.models.map(_.name))

    nameErrors ++ fieldErrors ++ duplicates
  }

  private def validateEnums(): Seq[String] = {
    val nameErrors = service.enums.flatMap { enum =>
      Text.validateName(enum.name) match {
        case Nil => None
        case errors => {
          Some(s"Enum[${enum.name}] name is invalid: ${errors.mkString(" ")}")
        }
      }
    }

    val duplicates = dupsError("Enum", service.enums.map(_.name))

    val valueErrors = service.enums.filter { _.values.isEmpty }.map { enum =>
      s"Enum[${enum.name}] must have at least one value"
    }

    val valuesWithInvalidNames = service.enums.flatMap { enum =>
      enum.values.filter(v => !v.name.isEmpty && !Text.startsWithLetter(v.name)).map { value =>
        s"Enum[${enum.name}] value[${value.name}] is invalid: must start with a letter"
      }
    }

    val duplicateValues = service.enums.flatMap { enum =>
      dups(enum.values.map(_.name)).map { value =>
        s"Enum[${enum.name}] value[$value] appears more than once"
      }
    }

    nameErrors ++ duplicates ++ valueErrors ++ valuesWithInvalidNames ++ duplicateValues
  }

  private def validateUnions(): Seq[String] = {
    val nameErrors = service.unions.flatMap { union =>
      Text.validateName(union.name) match {
        case Nil => None
        case errors => {
          Some(s"Union[${union.name}] name is invalid: ${errors.mkString(" ")}")
        }
      }
    }

    val typeErrors = service.unions.filter { _.types.isEmpty }.map { union =>
      s"Union[${union.name}] must have at least one type"
    }

    val invalidTypes = service.unions.filter(!_.name.isEmpty).flatMap { union =>
      union.types.flatMap { t =>
        typeResolver.parse(t.`type`) match {
          case None => Seq(s"Union[${union.name}] type[${t.`type`}] not found")
          case Some(t: Datatype) => {
            t.`type` match {
              case Type(Kind.Primitive, "unit") => {
                Seq("Union types cannot contain unit. To make a particular field optional, use the required property.")
              }
              case _ => {
                Seq.empty
              }
            }
          }
        }
      }
    }

    val duplicates = dupsError("Union", service.unions.map(_.name))

    nameErrors ++ typeErrors ++ invalidTypes ++ duplicates
  }

  private def validateHeaders(): Seq[String] = {
    val enumNames = service.enums.map(_.name).toSet

    val headersWithInvalidTypes = service.headers.flatMap { header =>
      typeResolver.parse(header.`type`) match {
        case None => Some(s"Header[${header.name}] type[${header.`type`}] is invalid")
        case Some(dt) => {
          dt.`type` match {
            case Type(Kind.Primitive, "string") => None
            case Type(Kind.Enum, _) => None
            case Type(Kind.Model | Kind.Union | Kind.Primitive, _) => {
              Some(s"Header[${header.name}] type[${header.`type`}] is invalid: Must be a string or the name of an enum")
            }
          }
        }
      }
    }

    val duplicates = dupsError("Header", service.headers.map(_.name))

    headersWithInvalidTypes ++ duplicates
  }

  /**
    * While not strictly necessary, we do this to reduce
    * confusion. Otherwise we would require an extension to
    * always indicate if a type referenced a model, union, or enum.
    */
  private def validateModelAndEnumAndUnionNamesAreDistinct(): Seq[String] = {
    val modelNames = service.models.map(_.name.toLowerCase)
    val enumNames = service.enums.map(_.name.toLowerCase)
    val unionNames = service.unions.map(_.name.toLowerCase)

    modelNames.filter { enumNames.contains(_) }.map { name =>
      s"Name[$name] cannot be used as the name of both a model and an enum"
    } ++ modelNames.filter { unionNames.contains(_) }.map { name =>
      s"Name[$name] cannot be used as the name of both a model and a union type"
    } ++ enumNames.filter { unionNames.contains(_) }.map { name =>
      s"Name[$name] cannot be used as the name of both an enum and a union type"
    }
  }

  private def validateFields(): Seq[String] = {
    service.models.flatMap { model =>
      model.fields.flatMap { field =>
        Text.validateName(field.name) match {
          case Nil => None
          case errors => {
            Some(s"Model[${model.name}] field name[${field.name}] is invalid: ${errors.mkString(" ")}")
          }
        }
      }
    }
  }

  private def dupsError(label: String, values: Iterable[String]): Seq[String] = {
    dups(values).map { n =>
      s"$label[$n] appears more than once"
    }.toSeq
  }

  private def dups(values: Iterable[String]): Iterable[String] = {
    values.groupBy(_.toLowerCase).filter { _._2.size > 1 }.keys
  }

}
