package controllers

import com.bryzek.apidoc.api.v0.models.{UserForm, UserUpdateForm}
import models.MainTemplate
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._
import java.util.UUID

import scala.concurrent.Future

object AccountProfileController extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def redirect = Action { implicit request =>
    Redirect(routes.AccountProfileController.index())
  }

  def index() = Authenticated { implicit request =>
    val tpl = request.mainTemplate(Some("Profile"))
    Ok(views.html.account.profile.index(tpl, request.user))
  }

  def edit() = Authenticated { implicit request =>
    val form = profileForm.fill(
      ProfileData(
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

    val form = profileForm.bindFromRequest
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

  case class ProfileData(
    email: String,
    nickname: String,
    name: Option[String]
  )

  private[this] val profileForm = Form(
    mapping(
      "email" -> nonEmptyText,
      "nickname" -> nonEmptyText,
      "name" -> optional(text)
    )(ProfileData.apply)(ProfileData.unapply)
  )
}
