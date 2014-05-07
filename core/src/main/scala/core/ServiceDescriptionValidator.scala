package core

import play.api.libs.json.JsValue
import com.fasterxml.jackson.core.{ JsonParseException, JsonProcessingException }
import com.fasterxml.jackson.databind.JsonMappingException

case class ServiceDescriptionValidator(apiJson: String) {

  private val RequiredFields = Seq("base_url", "name", "resources")

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
          validateResources ++ validateReferences ++ validateResponses
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
    internalServiceDescription.get.resources.flatMap { resource =>
      resource.fields.filter { f => !f.name.isEmpty }.flatMap { field =>
        field.references.flatMap { ref =>
          if (ref.resource.isEmpty || ref.field.isEmpty) {
            Some("Resource ${resource.name} field ${field.name.get} reference[${ref.label}] must contain a resource and field (e.g. users.guid)")

          } else {
            val matching = internalServiceDescription.get.resources.find { r => r.name == ref.resource.get }

            internalServiceDescription.get.resources.find { r => r.name == ref.resource.get } match {

              case None => Some(s"${resource.name}.${field.name.get} reference ${ref.label} is invalid. Resource[${ref.resource.get}] does not exist")

              case Some(r: InternalResource) => {
                r.fields.find(f => f.name == ref.field ) match {
                  case None => Some(s"${resource.name}.${field.name.get} reference ${ref.label} is invalid. Resource[${ref.resource.get}] does not have a field named[${ref.field.get}]")
                  case Some(f: InternalField) => None
                }
              }
            }
          }
        }
      }
    }
  }

  private def validateResources(): Seq[String] = {
    if (internalServiceDescription.get.resources.isEmpty) {
      Seq("Must have at least one resource")
    } else {
      internalServiceDescription.get.resources.flatMap { resource =>
        resource.fields match {
          case Nil => Some(s"${resource.name} resource must have at least one field")
          case fields =>
            fields.collect {
              case field if field.default.nonEmpty =>
                s"Field ${field.name} of resource ${resource.name} should not have a default attribute. Default is only valid on an operation parameter."
            }
        }
      }
    }
  }

  private def validateResponses(): Seq[String] = {
    internalServiceDescription.get.resources.flatMap { r =>
      r.operations.filter { op => !op.method.isEmpty && op.responses.isEmpty }.map { op =>
        val path = op.path match {
          case None => ""
          case Some(p: String) => s" $p"
        }
        s"${r.name} ${op.method.get}${path} missing responses element"
      }
    }
  }

}
