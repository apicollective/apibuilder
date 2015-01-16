package controllers

import lib.Role
import lib.{Pagination, PaginatedCollection, Role}
import models.{MainTemplate, SettingSection, SettingsMenu}
import com.gilt.apidoc.v0.models.{Organization, OrganizationForm, Visibility}
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
      applications <- request.api.Applications.getByOrgKey(
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
        request.mainTemplate().copy(title = Some(request.org.name)),
        applications = PaginatedCollection(page, applications),
        haveRequests = haveRequests)
      )
    }
  }

  def details(orgKey: String) = AuthenticatedOrg { implicit request =>
    val tpl = request.mainTemplate().copy(settings = Some(SettingsMenu(section = Some(SettingSection.Details))))
    Ok(views.html.organizations.details(tpl, request.org))
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
        request.mainTemplate(Some(request.org.name)),
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
            request.mainTemplate(Some(s"Join ${org.name}")),
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
    val filledForm = orgForm.fill(
      OrgData(
        name = "",
        namespace = "",
        key = None,
        visibility = Visibility.Organization.toString
      )
    )

    Ok(views.html.organizations.create(request.mainTemplate(), filledForm))
  }

  def createPost = Authenticated.async { implicit request =>
    val tpl = request.mainTemplate(Some("Add Organization"))

    val form = orgForm.bindFromRequest
    form.fold (

      errors => Future {
        Ok(views.html.organizations.create(tpl, errors))
      },

      valid => {
        request.api.Organizations.post(
          OrganizationForm(
            name = valid.name,
            namespace = valid.namespace,
            key = valid.key,
            visibility = Some(Visibility(valid.visibility))
          )
        ).map { org =>
          Redirect(routes.Organizations.show(org.key)).flashing("success" -> "Org created")
        }.recover {
          case r: com.gilt.apidoc.v0.errors.ErrorsResponse => {
            Ok(views.html.organizations.create(tpl, form, r.errors.map(_.message)))
          }
        }
      }

    )
  }

  def edit(orgKey: String) = Authenticated.async { implicit request =>
    request.api.Organizations.getByKey(orgKey).map { orgOption =>
      orgOption match {
        case None => {
          Redirect(routes.ApplicationController.index()).flashing("warning" -> "Org not found")
        }
        case Some(org) => {
          val filledForm = orgForm.fill(
            OrgData(
              name = org.name,
              namespace = org.namespace,
              key = Some(org.key),
              visibility = org.visibility.toString
            )
          )
          Ok(views.html.organizations.edit(request.mainTemplate().copy(org = Some(org)), org, filledForm))
        }
      }
    }
  }

  def editPost(orgKey: String) = Authenticated.async { implicit request =>
    request.api.Organizations.getByKey(orgKey).flatMap { orgOption =>
      orgOption match {
        case None => Future {
          Redirect(routes.ApplicationController.index()).flashing("warning" -> "Org not found")
        }
        case Some(org) => {
          val tpl = request.mainTemplate(Some("Edit Organization"))

          val form = orgForm.bindFromRequest
          form.fold (

            errors => Future {
              Ok(views.html.organizations.edit(tpl, org, errors))
            },

            valid => {
              val updatedKey = valid.key.map(_.trim).getOrElse(org.key)

              request.api.Organizations.putByKey(
                key = org.key,
                organizationForm = OrganizationForm(
                  name = valid.name,
                  namespace = valid.namespace,
                  key = Some(updatedKey),
                  visibility = Some(Visibility(valid.visibility))
                )
              ).map { org =>
                Redirect(routes.Organizations.details(updatedKey)).flashing("success" -> "Org updated")
              }.recover {
                case r: com.gilt.apidoc.v0.errors.ErrorsResponse => {
                  Ok(views.html.organizations.edit(tpl, org, form, r.errors.map(_.message)))
                }
              }
            }
          )
        }
      }
    }
  }

  def deletePost(orgKey: String) = Authenticated.async { implicit request =>
    for {
      result <- request.api.Organizations.deleteByKey(orgKey)
    } yield {
      Redirect(routes.ApplicationController.index()).flashing("success" -> "Org deleted")
    }
  }

  case class OrgData(
    name: String,
    namespace: String,
    key: Option[String],
    visibility: String
  )
  private val orgForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "namespace" -> nonEmptyText,
      "key" -> optional(nonEmptyText),
      "visibility" -> nonEmptyText
    )(OrgData.apply)(OrgData.unapply)
  )

}
