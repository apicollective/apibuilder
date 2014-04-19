package core

import play.api.libs.json.JsValue

case class ServiceDescriptionValidator(apiJson: String) {

  private val RequiredFields = Seq("base_url", "name", "resources")

  lazy val serviceDescription: Option[ServiceDescription] = {
    try {
      Some(ServiceDescription(apiJson))
    } catch {
/* TODO
      case jpe: JsonParseException => {
        None
      }
*/

      case e: Throwable => {
       // TODO throw e
       None
      }
    }
  }

  lazy val errors: Seq[String] = {
    serviceDescription match {

      case None => {
        if (apiJson == "") {
          Seq("No Data")
        } else {
          Seq("Invalid JSON")
        }
      }

      case Some(sd: ServiceDescription) => {
        val requiredFieldErrors = validateRequiredFields()

        if (requiredFieldErrors.isEmpty) {
          validateResources ++ validateReferences
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
      (serviceDescription.get.json \ field).asOpt[JsValue] match {
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
    serviceDescription.get.resources.flatMap { resource =>
      resource.fields.flatMap { field =>
        field.references.flatMap { ref =>
          serviceDescription.get.resources.find { r => r.name == ref.resource } match {

            case None => Some(s"Reference ${ref.label} points to a non existent resource")

            case Some(r: Resource) => {
              r.fields.find(f => f.name == ref.field ) match {
                case None => Some(s"Reference ${ref.label} points to a non existent field")
                case Some(f: Field) => None
              }
            }
          }
        }
      }
    }.distinct
  }

  private def validateResources(): Seq[String] = {
    if (serviceDescription.get.resources.isEmpty) {
      Seq("Must have at least one resource")
    } else {
      serviceDescription.get.resources.flatMap { resource =>
        if (resource.fields.isEmpty) {
          Some(s"${resource.name} resource must have at least one field")
        } else {
          None
        }
      }
    }
  }

}

