package controllers

import java.util.UUID
import lib._
import models.{SettingSection, SettingsMenu}
import io.apibuilder.api.v0.models.{AppSortBy, Organization, OrganizationForm, SortOrder, Visibility}
import play.api.data._
import play.api.data.Forms._

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject
import io.apibuilder.api.v0.Client
import io.apibuilder.common.v0.models.MembershipRole
import play.api.mvc.{Action, AnyContent}

class Organizations @Inject() (
                                val apiBuilderControllerComponents: ApiBuilderControllerComponents,
                                apiClientProvider: ApiClientProvider,
                                config: Config
) extends ApiBuilderController {

  private[this] implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  private[this] val apiBuilderSupportEmail: String = config.requiredString("apibuilder.supportEmail")

  def show(orgKey: String, page: Int = 0, sortBy: Option[AppSortBy] = None, ord: Option[SortOrder] = None): Action[AnyContent] = AnonymousOrg.async { implicit request =>
    request.api.Applications.get(
      orgKey = orgKey,
      limit = Pagination.DefaultLimit+1,
      offset = page * Pagination.DefaultLimit,
      sortBy = sortBy,
      order = ord
    ).flatMap { applications =>
      hasMembershipRequests(request.api, request.requestData.isAdmin, request.org.guid).map { haveMembershipRequests =>
        Ok(
          views.html.organizations.show(
            request.mainTemplate().copy(title = Some(request.org.name)),
            applications = PaginatedCollection(page, applications),
            haveMembershipRequests = haveMembershipRequests,
            sortBy = sortBy,
            ord = ord
          )
        )
      }
    }
  }

  def details(orgKey: String): Action[AnyContent] = IdentifiedOrg.async { implicit request =>
    assert(request.org.key == orgKey, s"Invalid org: ${orgKey}")
    hasMembershipRequests(request.api, request.requestData.isAdmin, request.org.guid).map { haveMembershipRequests =>
      val tpl = request.mainTemplate().copy(settings = Some(SettingsMenu(section = Some(SettingSection.Details))))
      Ok(views.html.organizations.details(tpl, request.org, haveMembershipRequests = haveMembershipRequests))
    }
  }

  def membershipRequests(orgKey: String, page: Int = 0): Action[AnyContent] = IdentifiedOrg.async { implicit request =>
    request.withAdmin {
      for {
        requests <- request.api.MembershipRequests.get(
          orgKey = Some(orgKey),
          limit = Pagination.DefaultLimit + 1,
          offset = page * Pagination.DefaultLimit
        )
      } yield {
        Ok(views.html.organizations.membershipRequests(
          request.mainTemplate(Some(request.org.name)),
          requests = PaginatedCollection(page, requests))
        )
      }
    }
  }

  def requestMembership(orgKey: String): Action[AnyContent] = Identified.async { implicit request =>
    for {
      orgResponse <- request.api.Organizations.get(key = Some(orgKey))
      membershipsResponse <- request.api.Memberships.get(
        orgKey = Some(orgKey),
        userGuid = Some(request.user.guid),
        role = Some(MembershipRole.Member.toString)
      )
      membershipRequestResponse <- request.api.MembershipRequests.get(
        orgKey = Some(orgKey),
        userGuid = Some(request.user.guid),
        role = Some(MembershipRole.Member.toString)
      )
      adminsResponse <- request.api.Memberships.get(
        orgKey = Some(orgKey),
        role = Some(MembershipRole.Admin.toString)
      )
    } yield {
      orgResponse.headOption match {
        case None => {
          Redirect("/").flashing("warning" -> s"Organization $orgKey not found")
        }
        case Some(org: Organization) => {
          val isMember = membershipsResponse.nonEmpty
          val hasMembershipRequest = membershipRequestResponse.nonEmpty
          Ok(views.html.organizations.requestMembership(
            request.mainTemplate(Some(s"Join ${org.name}")),
            org,
            adminsResponse,
            hasMembershipRequest,
            isMember,
            apiBuilderSupportEmail
          ))
        }
      }
    }
  }

  def postRequestMembership(orgKey: String): Action[AnyContent] = Identified.async { implicit request =>
    request.api.Organizations.get(key = Some(orgKey)).flatMap { orgs =>
      orgs.headOption match {
        case None => Future {
          Redirect("/").flashing("warning" -> s"Organization $orgKey not found")
        }
        case Some(org: Organization) => {
          request.api.MembershipRequests.post(org.guid, request.user.guid, MembershipRole.Member.toString).map { _ =>
            Redirect(routes.Organizations.requestMembership(orgKey)).flashing(
              "success" -> s"We have submitted your membership request to join ${org.name}"
            )
          }
        }
      }
    }
  }

  def create(): Action[AnyContent] = Identified { implicit request =>
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

  def createPost: Action[AnyContent] = Identified.async { implicit request =>
    val tpl = request.mainTemplate(Some("Add Organization"))

    val form = Organizations.orgForm.bindFromRequest()
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

  def edit(orgKey: String): Action[AnyContent] = Identified.async { implicit request =>
    apiClientProvider.callWith404(request.api.Organizations.getByKey(orgKey)).map {
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

  def editPost(orgKey: String): Action[AnyContent] = Identified.async { implicit request =>
    apiClientProvider.callWith404(request.api.Organizations.getByKey(orgKey)).flatMap {
      case None => Future.successful {
        Redirect(routes.ApplicationController.index()).flashing("warning" -> "Org not found")
      }
      case Some(org) => {
        val tpl = request.mainTemplate(Some("Edit Organization"))

        val form = Organizations.orgForm.bindFromRequest()
        form.fold(

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
            ).map { _ =>
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

  def deletePost(orgKey: String): Action[AnyContent] = Identified.async { implicit request =>
    for {
      _ <- request.api.Organizations.deleteByKey(orgKey)
    } yield {
      Redirect(routes.ApplicationController.index()).flashing("success" -> "Org deleted")
    }
  }

  private[this] def hasMembershipRequests(api: Client, isAdmin: Boolean, orgGuid: UUID): Future[Boolean] = {
    if (isAdmin) {
      api.MembershipRequests.get(orgGuid = Some(orgGuid), limit = 1).map(_.nonEmpty)
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
