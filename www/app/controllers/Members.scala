package controllers

import core.{ Review, Role }
import lib.{ Pagination, PaginatedCollection }
import apidoc.models.{ Organization, User }
import models.MainTemplate
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
      org <- request.api.Organizations.get(key = Some(orgKey))
      members <- request.api.Memberships.get(orgKey = Some(orgKey),
                                             limit = Some(Pagination.DefaultLimit+1),
                                             offset = Some(page * Pagination.DefaultLimit))
    } yield {
      org.entity.headOption match {

        case None => Redirect("/").flashing("warning" -> "Organization not found")

        case Some(o: Organization) => {
          Ok(views.html.members.show(MainTemplate(title = o.name,
                                                  org = Some(o),
                                                  user = Some(request.user)),
                                     members = PaginatedCollection(page, members.entity),
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
          Await.result(request.api.Users.get(email = Some(valid.email)), 1500.millis).entity.headOption match {

            case None => {
              val filledForm = addMemberForm.fill(valid)
              Ok(views.html.members.add(tpl, filledForm, Some("No user found")))
            }

            case Some(user: User) => {
              val membershipRequest = Await.result(request.api.MembershipRequests.post(request.org.guid, user.guid, valid.role), 1500.millis).entity
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
    sys.error("TODO")
  }

  def postMakeAdmin(orgKey: String, guid: UUID) = AuthenticatedOrg.async { implicit request =>
    sys.error("TODO")
  }

  case class AddMemberData(role: String, email: String)
  private val addMemberForm = Form(
    mapping(
      "role" -> nonEmptyText,
      "email" -> nonEmptyText
    )(AddMemberData.apply)(AddMemberData.unapply)
  )

}
