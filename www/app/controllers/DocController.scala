package controllers

import play.api._
import play.api.mvc._

object DocController extends Controller {

  def redirect = Action { implicit request =>
    Redirect(routes.DocController.index)
  }

  def index = Action { implicit request =>
    Ok(views.html.doc.index())
  }

  def gettingStarted = Action { implicit request =>
    Ok(views.html.doc.gettingStarted())
  }

  def apiJson = Action { implicit request =>
    Ok(views.html.doc.apiJson())
  }

  def types = Action { implicit request =>
    Ok(views.html.doc.types())
  }

  def clients = Action { implicit request =>
    Ok(views.html.doc.clients())
  }

  def examples = Action { implicit request =>
    Ok(views.html.doc.examples())
  }

  def history = Action { implicit request =>
    Ok(views.html.doc.history())
  }

  def releaseNotes = Action { implicit request =>
    Ok(views.html.doc.releaseNotes())
  }

  def todo = Action { implicit request =>
    Ok(views.html.doc.todo())
  }

}
