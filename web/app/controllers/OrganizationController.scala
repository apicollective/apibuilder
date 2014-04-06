package controllers

import models.{ MainTemplate, Role, UserRole }
import lib.{ Pagination, PaginatedCollection, Path }
import db.{ Organization, OrganizationQuery, Membership, MembershipRequest, ServiceDao, ServiceQuery }

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

object OrganizationController extends Controller {

  def redirect = Action {
    Redirect("/")
  }

  /**
   * Creates membership request for this user if not
   * already a member or admin
   */
  def register(orgKey: String) = Authenticated { request =>
    Organization.findByKey(orgKey) match {

      case None => {
        Redirect(routes.Application.index()).flashing(
          "error" -> "Organization not found"
        )
      }

      case Some(org: Organization) => {
        Membership.findByOrganizationAndUserAndRole(org, request.user, Role.Member.key) match {

          case None => {
            MembershipRequest.upsert(request.user, org, request.user, Role.Member.key)
            Redirect(routes.Application.index()).flashing(
              "warning" -> "Membership request created and will be reviewed by the organization's administrator"
            )
          }

          case Some(_) => {
            Redirect(routes.OrganizationController.show(orgKey)).flashing("warning" -> "You are already a member of this organization")
          }

        }
      }

    }
  }

  def show(orgKey: String, page: Int = 0) = Authenticated { request =>
    Organization.findByKey(orgKey) match {

      case None => {
        Redirect("/").flashing( "warnings" -> "Organization not found" )
      }

      case Some(org: Organization) => {
        val services = ServiceDao.findAll(ServiceQuery(org = Some(org),
                                                       limit = Pagination.DefaultLimit+1,
                                                       offset = page * Pagination.DefaultLimit))
        val collection = PaginatedCollection(page, services)
        val tpl = MainTemplate(user = Some(request.user), org = Some(org), title = org.name)
        val role = UserRole(org, request.user)
        val requestsPendingApproval = MembershipRequest.findAllPendingApproval(request.user)

        Ok(views.html.organizations.show(tpl, collection, role, requestsPendingApproval))
      }

    }
  }


  case class OrgData(name: String)
  private val orgForm = Form(
    mapping(
      "name" -> nonEmptyText
    )(OrgData.apply)(OrgData.unapply)
  )

  def requestMembership(orgKey: String) = Authenticated { request =>
    Organization.findByKey(orgKey) match {

      case None => {
        Redirect("/").flashing(
          "success" -> s"Could not find organization ${orgKey}"
        )
      }

      case Some(org: Organization) => {
        MembershipRequest.upsert(request.user, org, request.user, Role.Member.key)
        Redirect("/").flashing(
          "success" -> s"We have submitted your membership request to join ${org.key}."
        )
      }

    }
  }

  def create() = Authenticated { request =>
    Ok(views.html.organizations.form())
  }

  def createPost = Authenticated { implicit request =>
    orgForm.bindFromRequest.fold (

      errors => {
        // TODO: Display errors
        // Ok(views.html.organizations.form(errors))
        Ok(views.html.organizations.form())
      },

      valid => {
        Organization.findByKey(valid.name) match {
          case None => {
            val org = Organization.createWithAdministrator(request.user, valid.name)
            Redirect(lib.Path.url(org))
          }

          case Some(org: Organization) => {
            val tpl = MainTemplate(user = Some(request.user), title = s"Organization ${org.name} already exists")
            Ok(views.html.organizations.orgExists(tpl, org))
          }
        }
      }

    )
  }

}
