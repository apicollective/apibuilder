package controllers

import core.{ Review, Role }
import lib.{ Pagination, PaginatedCollection }
import apidoc.models.{ Organization, User }
import models.MainTemplate
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.Await
import scala.concurrent.duration._

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
          // TODO: Check if this user is an admin in order to allow
          // the add member link

          Ok(views.html.members.show(MainTemplate(title = o.name,
                                                  org = Some(o),
                                                  user = Some(request.user)),
                                     members = PaginatedCollection(page, members.entity)))
        }
      }
    }
  }

  def add(orgKey: String) = AuthenticatedOrg.async { implicit request =>
    require(request.isAdmin, "You are not an administrator")

    for {
      orgOption <- request.api.Organizations.get(key = Some(orgKey))
    } yield {
      val org = orgOption.entity.headOption.getOrElse { sys.error("invalid org") }
      val filledForm = addMemberForm.fill(AddMemberData(role = Role.Member.key, email = ""))

      Ok(views.html.members.add(MainTemplate(title = s"#{org.name}: Add member",
                                             org = Some(org),
                                             user = Some(request.user)),
                                filledForm))
    }
  }

  def addPost(orgKey: String) = AuthenticatedOrg.async { implicit request =>
    require(request.isAdmin, "You are not an administrator")

    for {
      orgOption <- request.api.Organizations.get(key = Some(orgKey))
    } yield {
      val org = orgOption.entity.headOption.getOrElse { sys.error("invalid org") }

      val tpl = MainTemplate(title = "#{org.name}: Add member",
                             org = Some(org),
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
              val membershipRequest = Await.result(request.api.MembershipRequests.post(org.guid, user.guid, valid.role), 1500.millis).entity
              val review = Await.result(request.api.MembershipRequests.postAcceptByGuid(membershipRequest.guid), 1500.millis)
              Redirect(routes.Members.show(org.key)).flashing("success" -> s"${valid.role} added")
            }
          }
        }
      )
    }
  }

  case class AddMemberData(role: String, email: String)
  private val addMemberForm = Form(
    mapping(
      "role" -> nonEmptyText,
      "email" -> nonEmptyText
    )(AddMemberData.apply)(AddMemberData.unapply)
  )

}
