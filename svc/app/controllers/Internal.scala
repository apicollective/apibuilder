package controllers

import lib.RouteGenerator

import play.api._
import play.api.mvc._

object Internal extends Controller {

  def routes() = Action { request =>
    val generator = RouteGenerator.fromFile("./svc/api.json")
    Ok(generator.generate())
  }

  def healthcheck() = Action { request =>
    Ok("healthy")
  }

}
