package controllers

import play.api._
import play.api.mvc._

object DocController extends Controller {

  def redirect = Action {
    Redirect(routes.DocController.index)
  }

  def index = Action {
    Ok(views.html.doc.index())
  }

  def examples = Action {
    Ok(views.html.doc.examples())
  }

  def apiJson = Action {
    Ok(views.html.doc.apiJson())
  }

  def todo = Action {
    Ok(views.html.doc.todo())
  }

  def codeGeneration = Action {
    Ok(views.html.doc.codeGeneration())
  }

}
