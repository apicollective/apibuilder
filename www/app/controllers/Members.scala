package controllers

import core.{ Review, Role }
import lib.{ Pagination, PaginatedCollection }
import com.gilt.apidoc.models.{ Organization, User }
import models._
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import java.util.UUID

object Members extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def show(orgKey: String, page: Int = 0) = AuthenticatedOrg.async { implicit request =>
    for {
      orgs <- request.api.Organizations.get(key = Some(orgKey))
      members <- request.api.Memberships.get(orgKey = Some(orgKey),
                                             limit = Some(Pagination.DefaultLimit+1),
                                             offset = Some(page * Pagination.DefaultLimit))
    } yield {
      orgs.headOption match {

        case None => Redirect("/").flashing("warning" -> "Organization not found")

        case Some(o: Organization) => {
          Ok(views.html.members.show(MainTemplate(title = o.name,
                                                  org = Some(o),
                                                  user = Some(request.user),
                                                  settings = Some(SettingsMenu(section = Some(SettingSection.Members)))),
                                     members = PaginatedCollection(page, members),
                                     isAdmin = request.isAdmin))
        }
      }
    }
  }

  def add(orgKey: String) = AuthenticatedOrg { implicit request =>
    if (request.isAdmin) {
      val filledForm = addMemberForm.fill(AddMemberData(role = Role.Member.key, email = ""))

      Ok(views.html.members.add(MainTemplate(title = s"#{request.org.name}: Add member",
                                             org = Some(request.org),
                                             user = Some(request.user)),
                                filledForm))
    } else {
      Redirect(routes.Organizations.show(orgKey)).flashing("warning" -> s"Only an administrator of this organization can add members")
    }
  }

  def addPost(orgKey: String) = AuthenticatedOrg { implicit request =>
    if (!request.isAdmin) {
      Redirect(routes.Organizations.show(orgKey)).flashing("warning" -> s"Only an administrator of this organization can add members")
    } else {
      val tpl = MainTemplate(title = "#{request.org.name}: Add member",
                             org = Some(request.org),
                             user = Some(request.user))

      addMemberForm.bindFromRequest.fold (

        errors => {
          Ok(views.html.members.add(tpl, errors))
        },

        valid => {
          Await.result(request.api.Users.get(email = Some(valid.email)), 1500.millis).headOption match {

            case None => {
              val filledForm = addMemberForm.fill(valid)
              Ok(views.html.members.add(tpl, filledForm, Some("No user found")))
            }

            case Some(user: User) => {
              val membershipRequest = Await.result(request.api.MembershipRequests.post(request.org.guid, user.guid, valid.role), 1500.millis)
              val review = Await.result(request.api.MembershipRequests.postAcceptByGuid(membershipRequest.guid), 1500.millis)
              Redirect(routes.Members.show(request.org.key)).flashing("success" -> s"${valid.role} added")
            }
          }
        }

      )
    }
  }

  def postRemove(orgKey: String, guid: UUID) = AuthenticatedOrg.async { implicit request =>
    require(request.isAdmin, s"User is not an admin of org[$orgKey]")

    for {
      response <- request.api.Memberships.deleteByGuid(guid)
    } yield {
      Redirect(routes.Members.show(request.org.key)).flashing("success" -> s"Member removed")
    }
  }

  def postRevokeAdmin(orgKey: String, guid: UUID) = AuthenticatedOrg.async { implicit request =>
    for {
      membership <- request.api.Memberships.getByGuid(guid)
      memberships <- request.api.Memberships.get(orgKey = Some(orgKey), userGuid = Some(membership.get.user.guid))
    } yield {
      memberships.find(_.role == Role.Member.key) match {
        case None => createMembership(request.api, request.org, membership.get.user.guid, Role.Member)
        case Some(m) => {}
      }

      memberships.find(_.role == Role.Admin.key) match {
        case None => {}
        case Some(membership) => {
          Await.result(
            request.api.Memberships.deleteByGuid(membership.guid),
            1500.millis
          )
        }
      }

      Redirect(routes.Members.show(request.org.key)).flashing("success" -> s"Member's admin access has been revoked")
    }
  }

  def postMakeAdmin(orgKey: String, guid: UUID) = AuthenticatedOrg.async { implicit request =>
    for {
      membership <- request.api.Memberships.getByGuid(guid)
      memberships <- request.api.Memberships.get(orgKey = Some(orgKey), userGuid = Some(membership.get.user.guid))
    } yield {
      memberships.find(_.role == Role.Admin.key) match {
        case None => createMembership(request.api, request.org, membership.get.user.guid, Role.Admin)
        case Some(m) => {}
      }

      memberships.find(_.role == Role.Member.key) match {
        case None => {}
        case Some(membership) => {
          Await.result(
            request.api.Memberships.deleteByGuid(membership.guid),
            1500.millis
          )
        }
      }

      Redirect(routes.Members.show(request.org.key)).flashing("success" -> s"Member granted admin access")
    }
  }

  private def createMembership(api: com.gilt.apidoc.Client, org: Organization, userGuid: UUID, role: Role) {
    val membershipRequest = Await.result(
      api.MembershipRequests.post(orgGuid = org.guid, userGuid = userGuid, role = role.key),
      1500.millis
    )
    Await.result(
      api.MembershipRequests.postAcceptByGuid(membershipRequest.guid),
      1500.millis
    )
  }

  case class AddMemberData(role: String, email: String)
  private val addMemberForm = Form(
    mapping(
      "role" -> nonEmptyText,
      "email" -> nonEmptyText
    )(AddMemberData.apply)(AddMemberData.unapply)
  )

}
