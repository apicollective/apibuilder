package controllers

import javax.inject.Inject

import lib.Util
import play.api._
import play.api.mvc._

class DocController @Inject() (
  util: Util
) extends Controller {

  def redirect = Action { implicit request =>
    Redirect(routes.DocController.index)
  }

  def index = Anonymous { implicit request =>
    Ok(views.html.doc.index(util, request.user))
  }

  def start = Anonymous { implicit request =>
    Ok(views.html.doc.start(util, request.user))
  }

  def apiJson = Anonymous { implicit request =>
    Ok(views.html.doc.apiJson(util, request.user))
  }

  def attributes = Anonymous { implicit request =>
    Ok(views.html.doc.attributes(util, request.user))
  }

  def types = Anonymous { implicit request =>
    Ok(views.html.doc.types(util, request.user))
  }

  def examples = Anonymous { implicit request =>
    Ok(views.html.doc.examples(util, request.user))
  }

  def generators = Anonymous { implicit request =>
    Ok(views.html.doc.generators(util, request.user))
  }

  def history = Anonymous { implicit request =>
    Ok(views.html.doc.history(util, request.user))
  }

  def releaseNotes = Anonymous { implicit request =>
    Ok(views.html.doc.releaseNotes(util, request.user))
  }

  def playRoutesFile = Anonymous { implicit request =>
    Ok(views.html.doc.playRoutesFile(util, request.user))
  }

  def playUnionTypes = Anonymous { implicit request =>
    Ok(views.html.doc.playUnionTypes(util, request.user))
  }

  def apiTokens = Anonymous { implicit request =>
    Ok(views.html.doc.apiTokens(util, request.user))
  }

  def why = Anonymous { implicit request =>
    Ok(views.html.doc.why(util, request.user))
  }

}
