package builder

import core.{Importer, ServiceFetcher, Util}
import com.gilt.apidoc.spec.v0.models.Service
import lib.Text

case class ServiceSpecValidator(
  service: Service,
  fetcher: ServiceFetcher
) {

  lazy val errors: Seq[String] = {
    validateName() ++
    validateBaseUrl() ++
    validateImports() ++
    validateModels()
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

  private def validateImports(): Seq[String] = {
    service.imports.flatMap { imp =>
      Util.isValidUri(imp.uri) match {
        case false => Seq(s"imports.uri[${imp.uri}] is not a valid URI")
        case true => Importer(fetcher, imp.uri).validate  // TODO. need to cache somewhere to avoid a second lookup when parsing later
      }
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


}
