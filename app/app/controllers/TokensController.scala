package controllers

import java.util.UUID
import javax.inject.Inject
import io.apibuilder.api.v0.models.TokenForm
import lib.{ApiClientProvider, PaginatedCollection, Pagination}
import play.api.data.*
import play.api.data.Forms.*
import play.api.mvc.{Action, AnyContent}

import scala.concurrent.{ExecutionContext, Future}

class TokensController @Inject() (
                                   val apiBuilderControllerComponents: ApiBuilderControllerComponents,
                                   apiClientProvider: ApiClientProvider
) extends ApiBuilderController {

  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def redirect: Action[AnyContent] = Action { implicit request =>
    Redirect(routes.TokensController.index())
  }

  def index(page: Int = 0): Action[AnyContent] = Identified.async { implicit request =>
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

  def show(guid: UUID): Action[AnyContent] = Identified.async { implicit request =>
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
          Ok(views.html.tokens.show(request.mainTemplate(Some("View token")), cleartextToken))
        }
      }
    }
  }

  def cleartext(guid: UUID): Action[AnyContent] = Identified.async { implicit request =>
    for {
      cleartextOption <- apiClientProvider.callWith404(request.api.tokens.getCleartextByGuid(guid))
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

  def create(): Action[AnyContent] = Identified { implicit request =>
    Ok(views.html.tokens.create(request.mainTemplate(Some("Create token")), TokensController.tokenForm))
  }

  def postCreate: Action[AnyContent] = Identified.async { implicit request =>
    val tpl = request.mainTemplate(Some("Create token"))

    val form = TokensController.tokenForm.bindFromRequest()
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

  def postDelete(guid: UUID): Action[AnyContent] = Identified.async { implicit request =>
    for {
      _ <- request.api.tokens.deleteByGuid(guid)
    } yield {
      Redirect(routes.TokensController.index()).flashing("success" -> "Token deleted")
    }
  }
}

object TokensController {

  case class TokenData(
    description: Option[String]
  )
  object TokenData {
    def unapply(d: TokenData): Option[Option[String]] = Some(d.description)
  }

  private[controllers] val tokenForm = Form(
    mapping(
      "description" -> optional(nonEmptyText)
    )(TokenData.apply)(TokenData.unapply)
  )
}
