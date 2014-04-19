package controllers

import core.Role
import lib.{ Pagination, PaginatedCollection }
import models.MainTemplate
import client.Apidoc
import client.Apidoc.Organization
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.Await
import scala.concurrent.duration._

object Organizations extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def show(orgKey: String, page: Int = 0) = Authenticated.async { implicit request =>
    for {
      org <- request.client.organizations.findByKey(orgKey)
      services <- request.client.services.findAllByOrganizationKey(orgKey,
                                                                   limit = Pagination.DefaultLimit+1,
                                                                   offset = page * Pagination.DefaultLimit)
      requests <- request.client.membershipRequests.findAll(organization_key = Some(orgKey), can_be_reviewed_by_guid = Some(request.user.guid), limit = 1)
    } yield {
      org match {

        case None => Redirect("/").flashing("warning" -> "Organization not found")

        case Some(org: Organization) => {
          Ok(views.html.organizations.show(MainTemplate(title = org.name,
                                                        org = Some(org),
                                                        user = Some(request.user)),
                                           services = PaginatedCollection(page, services),
                                           haveRequests = !requests.isEmpty))
        }
      }
    }
  }

  def membershipRequests(orgKey: String, page: Int = 0) = Authenticated.async { implicit request =>
    for {
      org <- request.client.organizations.findByKey(orgKey)
      requests = request.client.membershipRequests.findAll(organization_key = Some(orgKey),
                                                           can_be_reviewed_by_guid = Some(request.user.guid),
                                                           limit = Pagination.DefaultLimit+1,
                                                           offset = page * Pagination.DefaultLimit)
    } yield {
      org match {

        case None => Redirect("/").flashing("warning" -> "Organization not found")

        case Some(org: Organization) => {
          // TODO: Why if requests still a Future here?
          val fetchedRequests = Await.result(requests, 1500 millis)
          Ok(views.html.organizations.membershipRequests(MainTemplate(title = org.name,
                                                                      org = Some(org),
                                                                      user = Some(request.user)),
                                                         requests = PaginatedCollection(page, fetchedRequests)))
        }
      }
    }
  }

  def requestMembership(orgKey: String) = Authenticated { implicit request =>
    val org = Await.result(request.client.organizations.findByKey(orgKey), 1500 millis)
    org match {

      case None => Redirect("/").flashing("warning" -> "Organization not found")

      case Some(o: Organization) => {
        Await.result(request.client.membershipRequests.create(o.guid, request.user.guid, Role.Member.key), 1500 millis)
        Redirect("/").flashing(
          "success" -> s"We have submitted your membership request to join ${o.name}"
        )
      }
    }
  }

  def create() = Authenticated { implicit request =>
    Ok(views.html.organizations.form(orgForm))
  }

  def createPost = Authenticated { implicit request =>
    orgForm.bindFromRequest.fold (

      errors => {
        Ok(views.html.organizations.form(errors))
      },

      valid => {
        Await.result(request.client.organizations.findByName(valid.name), 1500 millis) match {
          case None => {
            val org = Await.result(request.client.organizations.create(request.user, valid.name), 1500 millis)
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
