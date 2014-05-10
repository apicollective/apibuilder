package core

import play.api.libs.json.JsValue
import com.fasterxml.jackson.core.{ JsonParseException, JsonProcessingException }
import com.fasterxml.jackson.databind.JsonMappingException

case class ServiceDescriptionValidator(apiJson: String) {

  private val RequiredFields = Seq("base_url", "name")

  private var parseError: Option[String] = None

  lazy val serviceDescription: Option[ServiceDescription] = {
    internalServiceDescription.map { ServiceDescription(_) }
  }

  lazy val internalServiceDescription: Option[InternalServiceDescription] = {
    try {
      Some(InternalServiceDescription(apiJson))
    } catch {
      case e: JsonParseException => {
        parseError = Some(e.getMessage)
        None
      }
      case e: JsonProcessingException => {
        parseError = Some(e.getMessage)
        None
      }
      case e: JsonMappingException => {
        parseError = Some(e.getMessage)
        None
      }
      case e: Throwable => throw e
    }
  }

  lazy val errors: Seq[String] = {
    internalServiceDescription match {

      case None => {
        if (apiJson == "") {
          Seq("No Data")
        } else {
          Seq(parseError.getOrElse("Invalid JSON"))
        }
      }

      case Some(sd: InternalServiceDescription) => {
        val requiredFieldErrors = validateRequiredFields()

        if (requiredFieldErrors.isEmpty) {
          validateModels ++ validateFields ++ validateDatatypes ++ validateReferences ++ validateOperations
        } else {
          requiredFieldErrors
        }
      }
    }
  }

  def isValid: Boolean = {
    errors.isEmpty
  }


  /**
   * Validate basic structure, returning a list of error messages
   */
  private def validateRequiredFields(): Seq[String] = {
    val missing = RequiredFields.filter { field =>
      (internalServiceDescription.get.json \ field).asOpt[JsValue] match {
        case None => true
        case Some(_) => false
      }
    }
    if (missing.isEmpty) {
      Seq.empty
    } else {
      Seq("Missing: " + missing.mkString(", "))
    }
  }

  /**
   * Validate references to ensure they refer to proper data
   */
  private def validateReferences(): Seq[String] = {
    internalServiceDescription.get.models.flatMap { model =>
      model.fields.flatMap { field =>
        field.references.flatMap { ref =>
          if (ref.modelPlural.isEmpty || ref.fieldName.isEmpty) {
            Some("Model ${model.name} field ${field.name.get} reference[${ref.label}] must contain a model name and a field name (e.g. users.guid)")

          } else {
            internalServiceDescription.get.models.find { m => m.plural == ref.modelPlural.get } match {

              case None => Some(s"${model.name}.${field.name.get} has invalid reference to ${ref.label}. Model[${ref.modelPlural.get}] does not exist")

              case Some(refModel: InternalModel) => {
                refModel.fields.find(m => m.name == ref.fieldName ) match {
                  case None => Some(s"${model.name}.${field.name.get} has invalid reference to ${ref.label}. Model[${refModel.name}] does not have a field named[${ref.fieldName.get}]")

                  case Some(f: InternalField) => None
                }
              }

            }
          }
        }
      }
    }
  }

  private def validateModels(): Seq[String] = {
    val fieldErrors = internalServiceDescription.get.models.flatMap { model =>
      model.fields match {
        case Nil => Some(s"Model ${model.name} must have at least one field")
        case fields => None
      }
    }

    val allNames = internalServiceDescription.get.models.map(_.name)
    val uniqueNames = allNames.distinct
    val duplicateNameErrors = if (allNames.size > uniqueNames.size) {
      Seq("Model names must be unique") // TODO Better error msg
    } else {
      Seq.empty
    }

    fieldErrors ++ duplicateNameErrors
  }

  private def validateFields(): Seq[String] = {
    internalServiceDescription.get.models.flatMap { model =>
      model.fields.filter { f => f.datatype.isEmpty && f.references.isEmpty }.map { f =>
        s"Model[${model.name}] field[${f.name}] must have either a datatype or references element"
      }
    }
  }

  private def validateDatatypes(): Seq[String] = {
    val modelErrors = internalServiceDescription.get.models.flatMap { model =>
      model.fields.filter( !_.datatype.isEmpty ).flatMap { field =>
        Datatype.findByName(field.datatype.get) match {
          case None => Some(s"Invalid datatype[${field.datatype.get}] for ${model.name}.${field.name.get}. Must be one of: ${Datatype.All.map(_.name).mkString(" ")}")
          case Some(d: Datatype) => None
        }
      }
    }

    val parameterErrors = internalServiceDescription.get.operations.flatMap { op =>
      op.parameters.filter( !_.datatype.isEmpty ).flatMap { param =>
        Datatype.findByName(param.datatype.get) match {
          case None => Some(s"Invalid datatype[${param.datatype.get}] for parameter[${param.name}] in operation ${op.resourceName} ${op.method} ${op.path}. Must be one of: ${Datatype.All.mkString(" ")}")
          case Some(d: Datatype) => None
        }
      }
    }

    modelErrors ++ parameterErrors
  }

  private def validateOperations(): Seq[String] = {
    val modelNames = internalServiceDescription.get.models.map( _.plural ).toSet

    internalServiceDescription.get.operations.filter { op => !modelNames.contains(op.resourceName) }.map { op =>
      s"Could not find model for operation with key[{$op.resourceName}]"
    }
  }

}
