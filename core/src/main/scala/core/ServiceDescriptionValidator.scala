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
          validateModels ++ validateReferences ++ validateOperations
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
    internalServiceDescription.get.models.filter { !_.name.isEmpty }.flatMap { model =>
      model.fields.flatMap { field =>
        field.references.flatMap { ref =>
          if (ref.modelName.isEmpty || ref.fieldName.isEmpty) {
            Some("Model ${model.name} field ${field.name.get} reference[${ref.label}] must contain a model name and a field name (e.g. users.guid)")

          } else {
            internalServiceDescription.get.models.find { m => m.name == ref.modelName.get } match {

              case None => Some(s"${model.name}.${field.name.get} has invalid reference to ${ref.label}. Model[${ref.modelName.get}] does not exist")

              case Some(refModel: InternalModel) => {
                refModel.fields.find(m => m.name == ref.fieldName ) match {
                  case None => Some(s"${model.name}.${field.name.get} has invalid reference to ${ref.label}. Model[${ref.modelName.get}] does not have a field named[${ref.fieldName.get}]")

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
    internalServiceDescription.get.models.flatMap { model =>
      model.fields match {
        case Nil => Some(s"Model ${model.name} must have at least one field")
        case fields => None
      }
    }
  }

  private def validateOperations(): Seq[String] = {
    val modelNames = internalServiceDescription.get.models.map( _.plural ).toSet

    internalServiceDescription.get.operations.filter { op => !modelNames.contains(op.resourceName) }.map { op =>
      s"Could not find model for operation with key[{$op.resourceName}]"
    }
  }

}
