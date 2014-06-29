package controllers

import core.Role
import lib.{ Pagination, PaginatedCollection }
import models.MainTemplate
import apidoc.models.Organization
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

object Organizations extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def show(orgKey: String, page: Int = 0) = AuthenticatedOrg.async { implicit request =>
    for {
      services <- request.api.Services.getByOrgKey(
        orgKey = orgKey,
        limit = Some(Pagination.DefaultLimit+1),
        offset = Some(page * Pagination.DefaultLimit)
      )
    } yield {
      val haveRequests = if (request.isAdmin) {
        val pendingRequests = Await.result(request.api.MembershipRequests.get(orgGuid = Some(request.org.guid), limit = Some(1)), 1500.millis)
        !pendingRequests.entity.isEmpty
      } else {
        false
      }

      Ok(views.html.organizations.show(MainTemplate(title = request.org.name,
        org = Some(request.org),
        user = Some(request.user)),
        services = PaginatedCollection(page, services.entity),
        haveRequests = haveRequests)
      )
    }
  }

  def membershipRequests(orgKey: String, page: Int = 0) = AuthenticatedOrg.async { implicit request =>
    for {
      requests <- request.api.MembershipRequests.get(orgKey = Some(orgKey),
                                                     limit = Some(Pagination.DefaultLimit+1),
                                                     offset = Some(page * Pagination.DefaultLimit))
    } yield {
      // TODO: Make sure user is an admin
      Ok(views.html.organizations.membershipRequests(
        MainTemplate(
          title = request.org.name,
          org = Some(request.org),
          user = Some(request.user)
        ),
        requests = PaginatedCollection(page, requests.entity))
      )
    }
  }

  def requestMembership(orgKey: String) = Authenticated.async { implicit request =>
    for {
      orgResponse <- request.api.Organizations.get(key = Some(orgKey))
      membershipRequestResponse <- request.api.MembershipRequests.get(
        orgKey = Some(orgKey),
        userGuid = Some(request.user.guid),
        role = Some(Role.Member.key)
      )
    } yield {
      orgResponse.entity.headOption match {
        case None => {
          Redirect("/").flashing("warning" -> s"Organization $orgKey not found")
        }
        case Some(org: Organization) => {
          val hasMembershipRequest = !membershipRequestResponse.entity.isEmpty
          Ok(views.html.organizations.requestMembership(
            MainTemplate(title = s"Join ${org.name}"),
            org,
            hasMembershipRequest
          ))
        }
      }
    }
  }

  def postRequestMembership(orgKey: String) = Authenticated.async { implicit request =>
    for {
      orgResponse <- request.api.Organizations.get(key = Some(orgKey))
    } yield {
      orgResponse.entity.headOption match {
        case None => {
          Redirect("/").flashing("warning" -> s"Organization $orgKey not found")
        }
        case Some(org: Organization) => {
          Await.result(request.api.MembershipRequests.post(org.guid, request.user.guid, Role.Member.key), 1000.millis)
          Redirect("/").flashing(
            "success" -> s"We have submitted your membership request to join ${org.name}"
          )
        }
      }
    }
  }

  def create() = Authenticated { implicit request =>
    Ok(views.html.organizations.form(orgForm))
  }

  def createPost = Authenticated.async { implicit request =>
    val form = orgForm.bindFromRequest
    form.fold (

      errors => Future {
        Ok(views.html.organizations.form(errors))
      },

      valid => {
        request.api.Organizations.post(valid.name).map { r =>
          Redirect(routes.Organizations.show(r.entity.key))
        }.recover {
          case apidoc.FailedResponse(errors: Seq[apidoc.models.Error], 409) => {
            val tpl = MainTemplate(user = Some(request.user), title = s"Create Organization")
            Ok(views.html.organizations.form(form, Some(errors.map(_.message).mkString(", "))))
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
