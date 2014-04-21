package controllers

import core.{ Review, Role }
import lib.{ Pagination, PaginatedCollection }
import models.MainTemplate
import client.Apidoc
import client.Apidoc.{ Organization, User }
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.Await
import scala.concurrent.duration._

object Members extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def show(orgKey: String, page: Int = 0) = Authenticated.async { implicit request =>
    for {
      org <- request.client.organizations.findByKey(orgKey)
      members <- request.client.memberships.findAll(organization_key = Some(orgKey),
                                                    limit = Pagination.DefaultLimit+1,
                                                    offset = page * Pagination.DefaultLimit)
    } yield {
      org match {

        case None => Redirect("/").flashing("warning" -> "Organization not found")

        case Some(o: Organization) => {
          // TODO: Check if this user is an admin in order to allow
          // the add member link

          Ok(views.html.members.show(MainTemplate(title = o.name,
                                                  org = Some(o),
                                                  user = Some(request.user)),
                                     members = PaginatedCollection(page, members)))
        }
      }
    }
  }

  def add(orgKey: String) = Authenticated.async { implicit request =>
    for {
      orgOption <- request.client.organizations.findByKey(orgKey)
    } yield {
      val org = orgOption.getOrElse { sys.error("invalid org") }
      val filledForm = addMemberForm.fill(AddMemberData(role = Role.Member.key, email = ""))

      Ok(views.html.members.add(MainTemplate(title = s"#{org.name}: Add member",
                                             org = Some(org),
                                             user = Some(request.user)),
                                filledForm))
    }
  }

  def addPost(orgKey: String) = Authenticated.async { implicit request =>
    for {
      orgOption <- request.client.organizations.findByKey(orgKey)
    } yield {
      val org = orgOption.getOrElse { sys.error("invalid org") }

      val tpl = MainTemplate(title = "#{org.name}: Add member",
                             org = Some(org),
                             user = Some(request.user))

      addMemberForm.bindFromRequest.fold (

        errors => {
          Ok(views.html.members.add(tpl, errors))
        },

        valid => {
          Await.result(request.client.users.findByEmail(valid.email), 1500 millis) match {

            case None => {
              val filledForm = addMemberForm.fill(valid)
              Ok(views.html.members.add(tpl, filledForm, Some("No user found")))
            }

            case Some(user: User) => {
              val membershipRequest = Await.result(request.client.membershipRequests.create(org.guid, user.guid, valid.role), 1500 millis)
              val review = Await.result(request.client.membershipRequestReviews.post(membershipRequest.guid, Review.Accept.key), 1500 millis)
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
