package controllers

import com.gilt.apidoc.v0.models.{TokenForm}
import lib.{Pagination, PaginatedCollection}
import models.MainTemplate
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._
import java.util.UUID

import scala.concurrent.Future

object TokensController extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def redirect = Action { implicit request =>
    Redirect(routes.TokensController.index())
  }

  def index(page: Int = 0) = Authenticated.async { implicit request =>
    for {
      tokens <- request.api.tokens.getUsersByUserGuid(
        request.user.guid,
        limit = Some(Pagination.DefaultLimit+1),
        offset = Some(page * Pagination.DefaultLimit)
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

      cleartext <- request.api.tokens.getCleartextByGuid(tokens.headOption.map(_.guid).get)
    } yield {
      cleartext match {
        case None => {
          Redirect(routes.TokensController.index()).flashing("warnings" -> "Token not found")
        }
        case Some(cleartextToken) => {
          Ok(views.html.tokens.show(request.mainTemplate(Some("View token")), tokens.head, cleartextToken))
        }
      }
    }
  }

  def create() = Authenticated { implicit request =>
    Ok(views.html.tokens.create(request.mainTemplate(Some("Create token")), tokenForm))
  }

  def postCreate = Authenticated.async { implicit request =>
    val tpl = request.mainTemplate(Some("Create token"))

    val form = tokenForm.bindFromRequest
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
          case r: com.gilt.apidoc.v0.errors.ErrorsResponse => {
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

  case class TokenData(
    description: Option[String]
  )

  private val tokenForm = Form(
    mapping(
      "description" -> optional(nonEmptyText)
    )(TokenData.apply)(TokenData.unapply)
  )
}
