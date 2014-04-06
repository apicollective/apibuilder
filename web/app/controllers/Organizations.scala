package controllers

import core.Organization
import play.api._
import play.api.mvc._

object Organizations extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def show(orgKey: String, page: Int = 0) = Authenticated.async { request =>
    // TODO: Fetch all services
    for {
      org <- Apidoc.organizations.findByKey(orgKey)
    } yield {
      org match {
        case None => Ok("not found")
        case Some(org: Organization) => Ok(org.name)
      }
    }
  }

}
