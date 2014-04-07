package controllers

import models.MainTemplate
import core.Organization
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.Await
import scala.concurrent.duration._

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

  def requestMembership(orgKey: String) = Authenticated { request =>
    Await.result(Apidoc.organizations.findByKey(orgKey), 100 millis) match {
      case None => {
        Redirect("/").flashing(
          "success" -> s"Could not find organization ${orgKey}"
        )
      }

      case Some(org: Organization) => {
        // TODO: MembershipRequest.upsert(request.user, org, request.user, Role.Member.key)
        Redirect("/").flashing(
          "success" -> s"We have submitted your membership request to join ${org.key}."
        )
      }

    }
  }

  def create() = Authenticated { request =>
    Ok(views.html.organizations.form())
  }

  def createPost = Authenticated { implicit request =>
    orgForm.bindFromRequest.fold (

      errors => {
        // TODO: Display errors
        // Ok(views.html.organizations.form(errors))
        Ok(views.html.organizations.form())
      },

      valid => {
        Await.result(Apidoc.organizations.findByName(valid.name), 100 millis) match {
          case None => {
            val org = Await.result(Apidoc.organizations.create(request.user, valid.name), 100 millis)
            Redirect(routes.Organizations.show(org.key))
          }

          case Some(org: Organization) => {
            val tpl = MainTemplate(user = Some(request.user), title = s"Organization ${org.name} already exists")
            Ok(views.html.organizations.orgExists(tpl, org))
          }
        }
      }

    )
  }

  case class OrgData(name: String)
  private val orgForm = Form(
    mapping(
      "name" -> nonEmptyText
    )(OrgData.apply)(OrgData.unapply)
  )

}
