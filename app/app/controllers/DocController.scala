package controllers

import javax.inject.Inject

import lib.Util
import play.api.mvc._

class DocController @Inject() (
                                val apiBuilderControllerComponents: ApiBuilderControllerComponents,
                                util: Util
) extends ApiBuilderController {

  def redirect: Action[AnyContent] = Action { implicit request =>
    Redirect(routes.DocController.index)
  }

  def index: Action[AnyContent] = Anonymous { implicit request =>
    Ok(views.html.doc.index(util, request.user))
  }

  def start: Action[AnyContent] = Anonymous { implicit request =>
    Ok(views.html.doc.start(util, request.user))
  }

  def apiJson: Action[AnyContent] = Anonymous { implicit request =>
    Ok(views.html.doc.apiJson(util, request.user))
  }

  def interfaces: Action[AnyContent] = Anonymous { implicit request =>
    Ok(views.html.doc.interfaces(util, request.user))
  }

  def attributes: Action[AnyContent] = Anonymous { implicit request =>
    Ok(views.html.doc.attributes(util, request.user))
  }

  def types: Action[AnyContent] = Anonymous { implicit request =>
    Ok(views.html.doc.types(util, request.user))
  }

  def templates: Action[AnyContent] = Anonymous { implicit request =>
    Ok(views.html.doc.templates(util, request.user))
  }

  def examples: Action[AnyContent] = Anonymous { implicit request =>
    Ok(views.html.doc.examples(util, request.user))
  }

  def generators: Action[AnyContent] = Anonymous { implicit request =>
    Ok(views.html.doc.generators(util, request.user))
  }

  def history: Action[AnyContent] = Anonymous { implicit request =>
    Ok(views.html.doc.history(util, request.user))
  }

  def releaseNotes: Action[AnyContent] = Anonymous { implicit request =>
    Ok(views.html.doc.releaseNotes(util, request.user))
  }

  def playRoutesFile: Action[AnyContent] = Anonymous { implicit request =>
    Ok(views.html.doc.playRoutesFile(util, request.user))
  }

  def playUnionTypes: Action[AnyContent] = Anonymous { implicit request =>
    Ok(views.html.doc.playUnionTypes(util, request.user))
  }

  def apiTokens: Action[AnyContent] = Anonymous { implicit request =>
    Ok(views.html.doc.apiTokens(util, request.user))
  }

  def why: Action[AnyContent] = Anonymous { implicit request =>
    Ok(views.html.doc.why(util, request.user))
  }

}
