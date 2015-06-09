package controllers

import play.api._
import play.api.mvc._

object DocController extends Controller {

  def redirect = Action { implicit request =>
    Redirect(routes.DocController.index)
  }

  def index = Anonymous { implicit request =>
    Ok(views.html.doc.index(request.user))
  }

  def start = Anonymous { implicit request =>
    Ok(views.html.doc.start(request.user))
  }

  def apiJson = Anonymous { implicit request =>
    Ok(views.html.doc.apiJson(request.user))
  }

  def types = Anonymous { implicit request =>
    Ok(views.html.doc.types(request.user))
  }

  def clients = Anonymous { implicit request =>
    Ok(views.html.doc.clients(request.user))
  }

  def examples = Anonymous { implicit request =>
    Ok(views.html.doc.examples(request.user))
  }

  def generators = Anonymous { implicit request =>
    Ok(views.html.doc.generators(request.user))
  }

  def history = Anonymous { implicit request =>
    Ok(views.html.doc.history(request.user))
  }

  def releaseNotes = Anonymous { implicit request =>
    Ok(views.html.doc.releaseNotes(request.user))
  }

  def todo = Anonymous { implicit request =>
    Ok(views.html.doc.todo(request.user))
  }

  def playRoutesFile = Anonymous { implicit request =>
    Ok(views.html.doc.playRoutesFile(request.user))
  }

  def playUnionTypes = Anonymous { implicit request =>
    Ok(views.html.doc.playUnionTypes(request.user))
  }

}
