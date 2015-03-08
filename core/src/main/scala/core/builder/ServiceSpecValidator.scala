package builder

import core.{Importer, ServiceFetcher}
import com.gilt.apidoc.spec.v0.models.Service
import lib.{Datatype, DatatypeResolver, Kind, Text, Type}

case class ServiceSpecValidator(
  service: Service,
  fetcher: ServiceFetcher
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
    validateUnions()
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

    val duplicates = service.models.groupBy(_.name.toLowerCase).filter { _._2.size > 1 }.keys.map { modelName =>
      s"Model[$modelName] appears more than once"
    }

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

    val duplicates = service.enums.groupBy(_.name.toLowerCase).filter { _._2.size > 1 }.keys.map { enumName =>
      s"Enum[$enumName] appears more than once"
    }

    val valueErrors = service.enums.filter { _.values.isEmpty }.map { enum =>
      s"Enum[${enum.name}] must have at least one value"
    }

    val valuesWithInvalidNames = service.enums.flatMap { enum =>
      enum.values.filter(v => !v.name.isEmpty && !Text.startsWithLetter(v.name)).map { value =>
        s"Enum[${enum.name}] value[${value.name}] is invalid: must start with a letter"
      }
    }

    val duplicateValues = service.enums.flatMap { enum =>
      enum.values.groupBy(_.name.toLowerCase).filter { _._2.size > 1 }.keys.map { value =>
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

    val duplicates = service.unions.groupBy(_.name.toLowerCase).filter { _._2.size > 1 }.keys.map { unionName =>
      s"Union[$unionName] appears more than once"
    }

    nameErrors ++ typeErrors ++ invalidTypes ++ duplicates
  }

}
