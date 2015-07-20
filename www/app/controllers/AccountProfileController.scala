package controllers

import com.bryzek.apidoc.api.v0.models.{UserForm, UserUpdateForm}
import javax.inject.Inject
import play.api.data._
import play.api.data.Forms._
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.mvc.{Action, Controller}
import scala.concurrent.Future

class AccountProfileController @Inject() (val messagesApi: MessagesApi) extends Controller with I18nSupport {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def redirect = Action { implicit request =>
    Redirect(routes.AccountProfileController.index())
  }

  def index() = Authenticated { implicit request =>
    val tpl = request.mainTemplate(Some("Profile"))
    Ok(views.html.account.profile.index(tpl, request.user))
  }

  def edit() = Authenticated { implicit request =>
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

  def postEdit = Authenticated.async { implicit request =>
    val tpl = request.mainTemplate(Some("Edit Profile"))

    val form = AccountProfileController.profileForm.bindFromRequest
    form.fold (
      errors => Future {
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
        ).map { user =>
          Redirect(routes.AccountProfileController.index()).flashing("success" -> "Profile updated")
        }.recover {
          case r: com.bryzek.apidoc.api.v0.errors.ErrorsResponse => {
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
