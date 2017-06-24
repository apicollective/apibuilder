package controllers

import io.apibuilder.api.v0.models.TokenForm
import lib.{Pagination, PaginatedCollection}
import models.MainTemplate
import play.api.data._
import play.api.data.Forms._
import java.util.UUID

import scala.concurrent.Future

import javax.inject.Inject
import play.api._
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.mvc.{Action, Controller}

class TokensController @Inject() (val messagesApi: MessagesApi) extends Controller with I18nSupport {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def redirect = Action { implicit request =>
    Redirect(routes.TokensController.index())
  }

  def index(page: Int = 0) = Authenticated.async { implicit request =>
    for {
      tokens <- request.api.tokens.getUsersByUserGuid(
        request.user.guid,
        limit = Pagination.DefaultLimit+1,
        offset = page * Pagination.DefaultLimit
      )
    } yield {
      Ok(views.html.tokens.index(request.mainTemplate(Some("Tokens")), PaginatedCollection(page, tokens)))
    }
  }

  def show(guid: UUID) = Authenticated.async { implicit request =>
    for {
      tokens <- request.api.tokens.getUsersByUserGuid(
        request.user.guid,
        guid = Some(guid)
      )
    } yield {
      tokens.headOption match {
        case None => {
          Redirect(routes.TokensController.index()).flashing("warning" -> "Token not found")
        }
        case Some(cleartextToken) => {
          Ok(views.html.tokens.show(request.mainTemplate(Some("View token")), tokens.head))
        }
      }
    }
  }

  def cleartext(guid: UUID) = Authenticated.async { implicit request =>
    for {
      cleartextOption <- lib.ApiClient.callWith404(request.api.tokens.getCleartextByGuid(guid))
    } yield {
      cleartextOption match {
        case None => {
          Redirect(routes.TokensController.index()).flashing("warning" -> "Token not found")
        }
        case Some(cleartextToken) => {
          Ok(views.html.tokens.cleartext(request.mainTemplate(Some("View token")), guid, cleartextToken))
        }
      }
    }
  }

  def create() = Authenticated { implicit request =>
    Ok(views.html.tokens.create(request.mainTemplate(Some("Create token")), TokensController.tokenForm))
  }

  def postCreate = Authenticated.async { implicit request =>
    val tpl = request.mainTemplate(Some("Create token"))

    val form = TokensController.tokenForm.bindFromRequest
    form.fold (

      errors => Future {
        Ok(views.html.tokens.create(tpl, errors))
      },

      valid => {
        request.api.tokens.post(
          TokenForm(
            userGuid = request.user.guid,
            description = valid.description
          )
        ).map { token =>
          Redirect(routes.TokensController.show(token.guid)).flashing("success" -> "Token created")
        }.recover {
          case r: io.apibuilder.api.v0.errors.ErrorsResponse => {
            Ok(views.html.tokens.create(tpl, form, r.errors.map(_.message)))
          }
        }
      }

    )
  }

  def postDelete(guid: UUID) = Authenticated.async { implicit request =>
    for {
      result <- request.api.tokens.deleteByGuid(guid)
    } yield {
      Redirect(routes.TokensController.index()).flashing("success" -> "Token deleted")
    }
  }
}

object TokensController {

  case class TokenData(
    description: Option[String]
  )

  private[controllers] val tokenForm = Form(
    mapping(
      "description" -> optional(nonEmptyText)
    )(TokenData.apply)(TokenData.unapply)
  )
}
