package controllers

import core.Role
import lib.{ Pagination, PaginatedCollection }
import models.MainTemplate
import apidoc.models.Organization
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
      org <- request.api.Organizations.get(key = Some(orgKey))
      services <- request.api.Services.getByOrgKey(orgKey = orgKey,
                                                   limit = Some(Pagination.DefaultLimit+1),
                                                   offset = Some(page * Pagination.DefaultLimit))
      isAdmin <- request.api.Memberships.get(orgKey = Some(orgKey),
                                             userGuid = Some(request.user.guid),
                                             role = Some(Role.Admin.key),
                                             limit = Some(1))
    } yield {
      org.entity.headOption match {

        case None => Redirect("/").flashing("warning" -> "Organization not found")

        case Some(org: Organization) => {
          val haveRequests = if (isAdmin.entity.isEmpty) {
            false
          } else {
            val pendingRequests = Await.result(request.api.MembershipRequests.get(organizationGuid = Some(org.guid), limit = Some(1)), 1500.millis)
            !pendingRequests.entity.isEmpty
          }

          Ok(views.html.organizations.show(MainTemplate(title = org.name,
                                                        org = Some(org),
                                                        user = Some(request.user)),
                                           services = PaginatedCollection(page, services.entity),
                                           haveRequests = haveRequests))
        }
      }
    }
  }

  def membershipRequests(orgKey: String, page: Int = 0) = Authenticated.async { implicit request =>
    for {
      org <- request.api.Organizations.get(key = Some(orgKey))
      requests <- request.api.MembershipRequests.get(organizationKey = Some(orgKey),
                                                     limit = Some(Pagination.DefaultLimit+1),
                                                     offset = Some(page * Pagination.DefaultLimit))
    } yield {
      org.entity.headOption match {

        case None => Redirect("/").flashing("warning" -> "Organization not found")

        case Some(org: Organization) => {
          // TODO: Make sure user is an admin
          Ok(views.html.organizations.membershipRequests(MainTemplate(title = org.name,
                                                                      org = Some(org),
                                                                      user = Some(request.user)),
                                                         requests = PaginatedCollection(page, requests.entity)))
        }
      }
    }
  }

  def requestMembership(orgKey: String) = Authenticated { implicit request =>
    val org = Await.result(request.api.Organizations.get(key = Some(orgKey)), 1500.millis)
    org.entity.headOption match {

      case None => Redirect("/").flashing("warning" -> "Organization not found")

      case Some(o: Organization) => {
        Await.result(request.api.MembershipRequests.post(o.guid, request.user.guid, Role.Member.key), 1500.millis)
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
        Await.result(request.api.Organizations.get(name = Some(valid.name)), 1500.millis).entity.headOption match {
          case None => {
            val org = Await.result(request.api.Organizations.post(valid.name), 1500.millis).entity
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
