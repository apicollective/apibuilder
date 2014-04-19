package controllers

import lib.{ Pagination, PaginatedCollection }
import models.MainTemplate
import client.Apidoc
import client.Apidoc.Organization
import play.api._
import play.api.mvc._

object Members extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def show(orgKey: String, page: Int = 0) = Authenticated.async { implicit request =>
    for {
      org <- request.client.organizations.findByKey(orgKey)
      members <- request.client.memberships.findAll(organization_key = Some(orgKey),
                                                    limit = Pagination.DefaultLimit+1,
                                                    offset = page * Pagination.DefaultLimit)
    } yield {
      org match {

        case None => Redirect("/").flashing("warning" -> "Organization not found")

        case Some(o: Organization) => {
          Ok(views.html.members.show(MainTemplate(title = o.name,
                                                  org = Some(o),
                                                  user = Some(request.user)),
                                     members = PaginatedCollection(page, members)))
        }
      }
    }
  }

  def add(orgKey: String) = Authenticated.async { implicit request =>
    for {
      org <- request.client.organizations.findByKey(orgKey)
    } yield {
      Ok("TODO")
    }
  }

}
