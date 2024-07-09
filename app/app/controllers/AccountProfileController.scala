package controllers

import io.apibuilder.api.v0.models.UserUpdateForm

import javax.inject.Inject
import play.api.data.*
import play.api.data.Forms.*
import play.api.mvc.{Action, AnyContent}

import scala.concurrent.{ExecutionContext, Future}

class AccountProfileController @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents
) extends ApiBuilderController {

  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def redirect: Action[AnyContent] = Action { implicit request =>
    Redirect(routes.AccountProfileController.index())
  }

  def index(): Action[AnyContent] = Identified { implicit request =>
    val tpl = request.mainTemplate(Some("Profile"))
    Ok(views.html.account.profile.index(tpl, request.user))
  }

  def edit(): Action[AnyContent] = Identified { implicit request =>
    val form = AccountProfileController.profileForm.fill(
      AccountProfileController.ProfileData(
        email = request.user.email,
        nickname = request.user.nickname,
        name = request.user.name
      )
    )

    val tpl = request.mainTemplate(Some("Edit Profile"))
    Ok(views.html.account.profile.edit(tpl, request.user, form))
  }

  def postEdit: Action[AnyContent] = Identified.async { implicit request =>
    val tpl = request.mainTemplate(Some("Edit Profile"))

    val form = AccountProfileController.profileForm.bindFromRequest()
    form.fold (
      _ => Future {
        Ok(views.html.account.profile.edit(tpl, request.user, form))
      },

      valid => {
        request.api.users.putByGuid(
          request.user.guid,
          UserUpdateForm(
            email = valid.email,
            nickname = valid.nickname,
            name = valid.name
          )
        ).map { _ =>
          Redirect(routes.AccountProfileController.index()).flashing("success" -> "Profile updated")
        }.recover {
          case r: io.apibuilder.api.v0.errors.ErrorsResponse => {
            Ok(views.html.account.profile.edit(tpl, request.user, form, r.errors.map(_.message)))
          }
        }
      }

    )
  }

}

object AccountProfileController {

  case class ProfileData(
    email: String,
    nickname: String,
    name: Option[String]
  )

  private[controllers] val profileForm = Form(
    mapping(
      "email" -> nonEmptyText,
      "nickname" -> nonEmptyText,
      "name" -> optional(text)
    )(ProfileData.apply)(ProfileData.unapply)
  )
}
