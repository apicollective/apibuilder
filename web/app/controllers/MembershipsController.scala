package controllers

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
