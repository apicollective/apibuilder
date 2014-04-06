package controllers

import db.{ Organization, OrganizationQuery, Membership, MembershipRequest, ServiceDao, ServiceQuery }

import play.api._
import play.api.mvc._

object MembershipsController extends Controller {

  def approve(guid: String) = Authenticated { request =>
    Ok("TODO")
  }

  def decline(guid: String) = Authenticated { request =>
    Ok("TODO")
  }

}
