package controllers

import lib.Role
import lib.{Pagination, PaginatedCollection, Role}
import models.MainTemplate
import com.gilt.apidoc.models.Organization
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

object Organizations extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def show(orgKey: String, page: Int = 0) = AnonymousOrg.async { implicit request =>
    for {
      services <- request.api.Services.getByOrgKey(
        orgKey = orgKey,
        limit = Some(Pagination.DefaultLimit+1),
        offset = Some(page * Pagination.DefaultLimit)
      )
    } yield {
      val haveRequests = if (request.isAdmin) {
        val pendingRequests = Await.result(request.api.MembershipRequests.get(orgGuid = Some(request.org.guid), limit = Some(1)), 1500.millis)
        !pendingRequests.isEmpty
      } else {
        false
      }

      Ok(views.html.organizations.show(
        request.mainTemplate(),
        services = PaginatedCollection(page, services),
        haveRequests = haveRequests)
      )
    }
  }

  def membershipRequests(orgKey: String, page: Int = 0) = AuthenticatedOrg.async { implicit request =>
    request.requireAdmin

    for {
      requests <- request.api.MembershipRequests.get(
        orgKey = Some(orgKey),
        limit = Some(Pagination.DefaultLimit+1),
        offset = Some(page * Pagination.DefaultLimit)
      )
    } yield {
      Ok(views.html.organizations.membershipRequests(
        MainTemplate(
          user = Some(request.user),
          title = Some(request.org.name),
          org = Some(request.org)
        ),
        requests = PaginatedCollection(page, requests))
      )
    }
  }

  def requestMembership(orgKey: String) = Authenticated.async { implicit request =>
    for {
      orgResponse <- request.api.Organizations.get(key = Some(orgKey))
      membershipsResponse <- request.api.Memberships.get(
        orgKey = Some(orgKey),
        userGuid = Some(request.user.guid),
        role = Some(Role.Member.key)
      )
      membershipRequestResponse <- request.api.MembershipRequests.get(
        orgKey = Some(orgKey),
        userGuid = Some(request.user.guid),
        role = Some(Role.Member.key)
      )
      adminsResponse <- request.api.Memberships.get(
        orgKey = Some(orgKey),
        role = Some(Role.Admin.key)
      )
    } yield {
      orgResponse.headOption match {
        case None => {
          Redirect("/").flashing("warning" -> s"Organization $orgKey not found")
        }
        case Some(org: Organization) => {
          val isMember = !membershipsResponse.headOption.isEmpty
          val hasMembershipRequest = !membershipRequestResponse.isEmpty
          Ok(views.html.organizations.requestMembership(
            MainTemplate(
              user = Some(request.user),
              title = Some("Join ${org.name}")
            ),
            org,
            adminsResponse,
            hasMembershipRequest,
            isMember
          ))
        }
      }
    }
  }

  def postRequestMembership(orgKey: String) = Authenticated.async { implicit request =>
    request.api.Organizations.get(key = Some(orgKey)).flatMap { orgs =>
      orgs.headOption match {
        case None => Future {
          Redirect("/").flashing("warning" -> s"Organization $orgKey not found")
        }
        case Some(org: Organization) => {
          request.api.MembershipRequests.post(org.guid, request.user.guid, Role.Member.key).map { _ =>
            Redirect(routes.Organizations.requestMembership(orgKey)).flashing(
              "success" -> s"We have submitted your membership request to join ${org.name}"
            )
          }
        }
      }
    }
  }

  def create() = Authenticated { implicit request =>
    Ok(views.html.organizations.form(
      models.MainTemplate(
        user = Some(request.user),
        title = Some("Add Organization")
      ),
      orgForm
    ))
  }

  def createPost = Authenticated.async { implicit request =>
    val tpl = models.MainTemplate(
      user = Some(request.user),
      title = Some("Add Organization")
    )

    val form = orgForm.bindFromRequest
    form.fold (

      errors => Future {
        Ok(views.html.organizations.form(tpl, errors))
      },

      valid => {
        request.api.Organizations.post(valid.name).map { org =>
          Redirect(routes.Organizations.show(org.key))
        }.recover {
          case r: com.gilt.apidoc.error.ErrorsResponse => {
            Ok(views.html.organizations.form(tpl, form, Some(r.errors.map(_.message).mkString(", "))))
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
