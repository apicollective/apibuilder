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

  def examples = Action { implicit request =>
    Ok(views.html.doc.examples())
  }

  def apiJson = Action { implicit request =>
    Ok(views.html.doc.apiJson())
  }

  def todo = Action { implicit request =>
    Ok(views.html.doc.todo())
  }

  def codeGeneration = Action { implicit request =>
    Ok(views.html.doc.codeGeneration())
  }

}
