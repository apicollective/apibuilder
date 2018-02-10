package controllers

import java.util.UUID

import lib._
import models.{SettingSection, SettingsMenu}
import io.apibuilder.api.v0.models.{Organization, OrganizationForm, Visibility}
import play.api.data._
import play.api.data.Forms._

import scala.concurrent.Future
import javax.inject.Inject

import io.apibuilder.api.v0.Client
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Controller

class Organizations @Inject() (
  val messagesApi: MessagesApi,
  apiClientProvider: ApiClientProvider,
  config: Config
) extends Controller with I18nSupport {

  private[this] implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  val apibuilderSupportEmail: String = config.requiredString("apibuilder.supportEmail")

  def show(orgKey: String, page: Int = 0) = AnonymousOrg.async { implicit request =>
    request.api.Applications.get(
      orgKey = orgKey,
      limit = Pagination.DefaultLimit+1,
      offset = page * Pagination.DefaultLimit
    ).flatMap { applications =>
      hasMembershipRequests(request.api, request.isAdmin, request.org.guid).map { haveMembershipRequests =>
        Ok(
          views.html.organizations.show(
            request.mainTemplate().copy(title = Some(request.org.name)),
            applications = PaginatedCollection(page, applications),
            haveMembershipRequests = haveMembershipRequests
          )
        )
      }
    }
  }

  def details(orgKey: String) = AuthenticatedOrg.async { implicit request =>
    hasMembershipRequests(request.api, request.isAdmin, request.org.guid).map { haveMembershipRequests =>
      val tpl = request.mainTemplate().copy(settings = Some(SettingsMenu(section = Some(SettingSection.Details))))
      Ok(views.html.organizations.details(tpl, request.org, haveMembershipRequests = haveMembershipRequests))
    }
  }

  def membershipRequests(orgKey: String, page: Int = 0) = AuthenticatedOrg.async { implicit request =>
    request.requireAdmin

    for {
      requests <- request.api.MembershipRequests.get(
        orgKey = Some(orgKey),
        limit = Pagination.DefaultLimit+1,
        offset = page * Pagination.DefaultLimit
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
            isMember,
            apibuilderSupportEmail
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
    val filledForm = Organizations.orgForm.fill(
      Organizations.OrgData(
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

    val form = Organizations.orgForm.bindFromRequest
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
            visibility = Visibility(valid.visibility)
          )
        ).map { org =>
          Redirect(routes.Organizations.show(org.key)).flashing("success" -> "Org created")
        }.recover {
          case r: io.apibuilder.api.v0.errors.ErrorsResponse => {
            Ok(views.html.organizations.create(tpl, form, r.errors.map(_.message)))
          }
        }
      }

    )
  }

  def edit(orgKey: String) = Authenticated.async { implicit request =>
    apiClientProvider.callWith404(request.api.Organizations.getByKey(orgKey)).map { orgOption =>
      orgOption match {
        case None => {
          Redirect(routes.ApplicationController.index()).flashing("warning" -> "Org not found")
        }
        case Some(org) => {
          val filledForm = Organizations.orgForm.fill(
            Organizations.OrgData(
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
    apiClientProvider.callWith404(request.api.Organizations.getByKey(orgKey)).flatMap { orgOption =>
      orgOption match {
        case None => Future {
          Redirect(routes.ApplicationController.index()).flashing("warning" -> "Org not found")
        }
        case Some(org) => {
          val tpl = request.mainTemplate(Some("Edit Organization"))

          val form = Organizations.orgForm.bindFromRequest
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
                  visibility = Visibility(valid.visibility)
                )
              ).map { org =>
                Redirect(routes.Organizations.details(updatedKey)).flashing("success" -> "Org updated")
              }.recover {
                case r: io.apibuilder.api.v0.errors.ErrorsResponse => {
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
      _ <- request.api.Organizations.deleteByKey(orgKey)
    } yield {
      Redirect(routes.ApplicationController.index()).flashing("success" -> "Org deleted")
    }
  }

  private[this] def hasMembershipRequests(api: Client, isAdmin: Boolean, orgGuid: UUID): Future[Boolean] = {
    if (isAdmin) {
      api.MembershipRequests.get(orgGuid = Some(orgGuid), limit = 1).map(!_.isEmpty)
    } else {
      Future.successful(false)
    }
  }

}

object Organizations {

  case class OrgData(
    name: String,
    namespace: String,
    key: Option[String],
    visibility: String
  )

  private[controllers] val orgForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "namespace" -> nonEmptyText,
      "key" -> optional(nonEmptyText),
      "visibility" -> nonEmptyText
    )(OrgData.apply)(OrgData.unapply)
  )

}
