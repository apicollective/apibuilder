package controllers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import play.api.mvc._
import play.api.libs.json._

object Code extends Controller {

  def generate(organization: String, service: String, versionName: String, target: String) = Authenticated.async { request =>
    println("organization: " + organization)
    println("service: " + service)
    println("versionName: " + versionName)
    println("target: " + target)

    for {
      versionResponse <- request.api.Versions.getByOrgKeyAndServiceKeyAndVersion(organization, service, versionName)
      codeResponse <- request.api.Code.getByVersionAndTarget(versionResponse.entity.guid.toString, target)
    } yield {
      println("versionResponse: " + versionResponse)
      println("codeResponse: " + codeResponse)
      // TODO: Handle 404
      Ok(codeResponse.entity.source)
    }
  }

}
