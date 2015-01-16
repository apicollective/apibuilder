package controllers

import com.gilt.apidoc.v0.models.{PasswordReset, PasswordResetRequest}
import models.MainTemplate
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.Future

object LoginController extends Controller {

  import scala.concurrent.ExecutionContext.Implicits.global

  def redirect = Action {
    Redirect(routes.LoginController.index())
  }

  def index(returnUrl: Option[String]) = Action { implicit request =>
    val tpl = MainTemplate(requestPath = request.path)
    val lForm = loginForm.fill(LoginData(email = "", password = "", returnUrl = returnUrl))
    val rForm = registerForm.fill(RegisterData(name = None, email = "", password = "", passwordVerify = "", returnUrl = returnUrl))
    Ok(views.html.login.index(tpl, Tab.Login, lForm, rForm))
  }

  def indexPost = Action.async { implicit request =>
    val tpl = MainTemplate(requestPath = request.path)
    val form = loginForm.bindFromRequest
    form.fold (

      formWithErrors => Future {
        Ok(views.html.login.index(tpl, Tab.Login, formWithErrors, registerForm))
      },

      validForm => {
        val returnUrl = validForm.returnUrl.getOrElse("/")
        Authenticated.api().Users.postAuthenticate(email = validForm.email, password = validForm.password).map { user =>
          Redirect(returnUrl).withSession { "user_guid" -> user.guid.toString }
        }.recover {
          case r: com.gilt.apidoc.v0.error.ErrorsResponse => {
            Ok(views.html.login.index(tpl, Tab.Login, form, registerForm, Some(r.errors.map(_.message).mkString(", "))))
          }
        }
      }

    )
  }

  def registerPost = Action.async { implicit request =>
    val tpl = MainTemplate(requestPath = request.path)

    val form = registerForm.bindFromRequest
    form.fold (

      formWithErrors => Future {
        Ok(views.html.login.index(tpl, Tab.Register, loginForm, formWithErrors))
      },

      validForm => {
        val returnUrl = validForm.returnUrl.getOrElse("/")
        Authenticated.api().Users.post(name = validForm.name, email = validForm.email, password = validForm.password).map { user =>
          Redirect(returnUrl).withSession { "user_guid" -> user.guid.toString }
        }.recover {
          case r: com.gilt.apidoc.v0.error.ErrorsResponse => {
            Ok(views.html.login.index(tpl, Tab.Register, loginForm, form, Some(r.errors.map(_.message).mkString(", "))))
          }
        }
      }

    )
  }

  def forgotPassword() = Action { implicit request =>
    val tpl = MainTemplate(requestPath = request.path)
    Ok(views.html.login.forgotPassword(tpl, forgotPasswordForm))
  }

  def postForgotPassword = Action.async { implicit request =>
    val tpl = MainTemplate(requestPath = request.path)
    val form = forgotPasswordForm.bindFromRequest
    form.fold (

      formWithErrors => Future {
        Ok(views.html.login.forgotPassword(tpl, formWithErrors))
      },

      validForm => {
        Authenticated.api().passwordResetRequests.post(
          passwordResetRequest = PasswordResetRequest(email = validForm.email)
        ).map { _ =>
          Ok(views.html.login.forgotPasswordConfirmation(tpl, validForm.email))
        }

      }

    )
  }

  def resetPassword(token: String) = Action { implicit request =>
    val tpl = MainTemplate(requestPath = request.path)
    Ok(views.html.login.resetPassword(tpl, token, resetPasswordForm))
  }

  def postResetPassword(token: String) = Action.async { implicit request =>
    val tpl = MainTemplate(requestPath = request.path)
    val form = resetPasswordForm.bindFromRequest
    form.fold (

      formWithErrors => Future {
        Ok(views.html.login.resetPassword(tpl, token, formWithErrors))
      },

      validForm => {
        Authenticated.api().passwordResets.post(
          passwordReset = PasswordReset(token = token, password = validForm.password)
        ).map { result =>
          Redirect("/").
            withSession { "user_guid" -> result.userGuid.toString }.
            flashing("success" -> "Your password has been reset and you are now logged in")
        
        }.recover {
          case r: com.gilt.apidoc.v0.error.ErrorsResponse => {
            Ok(views.html.login.resetPassword(tpl, token, form, Some(r.errors.map(_.message).mkString(", "))))
          }
        }
      }

    )
  }

  case class LoginData(email: String, password: String, returnUrl: Option[String])
  val loginForm = Form(
    mapping(
      "email" -> nonEmptyText,
      "password" -> nonEmptyText,
      "return_url" -> optional(text)
    )(LoginData.apply)(LoginData.unapply)
  )

  case class RegisterData(name: Option[String], email: String, password: String, passwordVerify: String, returnUrl: Option[String])
  val registerForm = Form(
    mapping(
      "name" -> optional(text),
      "email" -> nonEmptyText,
      "password" -> nonEmptyText(minLength=5),
      "password_verify" -> nonEmptyText,
      "return_url" -> optional(text)
    )(RegisterData.apply)(RegisterData.unapply) verifying("Password and password verify do not match", { f =>
      f.password == f.passwordVerify
    })
  )

  case class ForgotPasswordData(email: String)
  val forgotPasswordForm = Form(
    mapping(
      "email" -> nonEmptyText
    )(ForgotPasswordData.apply)(ForgotPasswordData.unapply)
  )

  case class ResetPasswordData(password: String, passwordVerify: String)
  val resetPasswordForm = Form(
    mapping(
      "password" -> nonEmptyText(minLength=5),
      "password_verify" -> nonEmptyText
    )(ResetPasswordData.apply)(ResetPasswordData.unapply) verifying("Password and password verify do not match", { f =>
      f.password == f.passwordVerify
    })
  )

  sealed trait Tab
  object Tab {

    case object Login extends Tab
    case object Register extends Tab

  }

}
