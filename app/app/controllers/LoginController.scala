package controllers

import io.apibuilder.api.v0.models.{PasswordReset, PasswordResetRequest}
import lib.{ApiClientProvider, Github, Util}
import models.MainTemplate
import play.api.data.*
import play.api.data.Forms.*

import scala.concurrent.Future
import javax.inject.Inject
import play.api.Logger
import play.api.mvc.{Action, AnyContent}

class LoginController @Inject() (
                                  val apiBuilderControllerComponents: ApiBuilderControllerComponents,
                                  apiClientProvider: ApiClientProvider,
                                  github: Github,
                                  util: Util
) extends ApiBuilderController {

  private val logger: Logger = Logger(this.getClass)

  import scala.concurrent.ExecutionContext.Implicits.global

  private val DevSessionId = "dev"

  def redirect: Action[AnyContent] = Action {
    Redirect(routes.LoginController.index())
  }

  def developmentLogin: Action[AnyContent] = Anonymous.async { request =>
    request.api.authentications.getSessionById(DevSessionId).map { result =>
      Redirect("/").
        withSession { "session_id" -> result.session.id }.
        flashing("success" -> s"You have been logged in as ${result.user.nickname}")

    }.recover {
      case io.apibuilder.api.v0.errors.UnitResponse(404) => {
        Redirect("/").
          flashing("warning" -> "Developer login not enabled")
      }
    }
  }

  def index(returnUrl: Option[String]): Action[AnyContent] = Action { implicit request =>
    val tpl = MainTemplate(requestPath = request.path)
    Ok(views.html.login.index(tpl, github, LoginController.Tab.Login, returnUrl))
  }

  def legacy(returnUrl: Option[String]): Action[AnyContent] = Action { implicit request =>
    val tpl = MainTemplate(requestPath = request.path)
    val lForm = LoginController.loginForm.fill(LoginController.LoginData(email = "", password = "", returnUrl = returnUrl))
    val rForm = LoginController.registerForm.fill(LoginController.RegisterData(name = None, email = "", password = "", passwordVerify = "", returnUrl = returnUrl))
    Ok(views.html.login.legacy(tpl, LoginController.Tab.Login, lForm, rForm))
  }

  def legacyPost: Action[AnyContent] = Anonymous.async { implicit request =>
    val tpl = MainTemplate(requestPath = request.path)
    val form = LoginController.loginForm.bindFromRequest()
    form.fold (

      formWithErrors => Future.successful {
        Ok(views.html.login.legacy(tpl, LoginController.Tab.Login, formWithErrors, LoginController.registerForm))
      },

      validForm => {
        val url = validForm.returnUrl match {
          case None => {
            routes.ApplicationController.index().path
          }
          case Some(u) => {
            util.validateReturnUrl(u) match {
              case Left(errors) => {
                logger.warn(s"[LoginController] Invalid redirect url[$u] Ignoring and redirecting to '/': $errors")
                "/"
              }
              case Right(validUrl) => {
                validUrl
              }
            }
          }
        }

        request.api.Users.postAuthenticate(email = validForm.email, password = validForm.password).map { auth =>
          Redirect(url).withSession { "session_id" -> auth.session.id }
        }.recover {
          case r: io.apibuilder.api.v0.errors.ErrorsResponse => {
            Ok(views.html.login.legacy(tpl, LoginController.Tab.Login, form, LoginController.registerForm, Some(r.errors.map(_.message).mkString(", "))))
          }
        }
      }

    )
  }

  def forgotPassword(): Action[AnyContent] = Action { implicit request =>
    val tpl = MainTemplate(requestPath = request.path)
    Ok(views.html.login.forgotPassword(tpl, LoginController.forgotPasswordForm))
  }

  def postForgotPassword: Action[AnyContent] = Anonymous.async { implicit request =>
    val tpl = MainTemplate(requestPath = request.path)
    val form = LoginController.forgotPasswordForm.bindFromRequest()
    form.fold (

      formWithErrors => Future {
        Ok(views.html.login.forgotPassword(tpl, formWithErrors))
      },

      validForm => {
        request.api.passwordResetRequests.post(
          passwordResetRequest = PasswordResetRequest(email = validForm.email)
        ).map { _ =>
          Ok(views.html.login.forgotPasswordConfirmation(tpl, validForm.email))
        }

      }

    )
  }

  def resetPassword(token: String): Action[AnyContent] = Action { implicit request =>
    val tpl = MainTemplate(requestPath = request.path)
    Ok(views.html.login.resetPassword(tpl, token, LoginController.resetPasswordForm))
  }

  def postResetPassword(token: String): Action[AnyContent] = Anonymous.async { implicit request =>
    val tpl = MainTemplate(requestPath = request.path)
    val form = LoginController.resetPasswordForm.bindFromRequest()
    form.fold (

      formWithErrors => Future {
        Ok(views.html.login.resetPassword(tpl, token, formWithErrors))
      },

      validForm => {
        request.api.passwordResets.post(
          passwordReset = PasswordReset(token = token, password = validForm.password)
        ).map { result =>
          Redirect("/").
            withSession { "session_id" -> result.session.id }.
            flashing("success" -> "Your password has been reset and you are now logged in")
        
        }.recover {
          case r: io.apibuilder.api.v0.errors.ErrorsResponse => {
            Ok(views.html.login.resetPassword(tpl, token, form, Some(r.errors.map(_.message).mkString(", "))))
          }
        }
      }

    )
  }
}

object LoginController {

  case class LoginData(email: String, password: String, returnUrl: Option[String])
  object LoginData {
    def unapply(d: LoginData): Option[(String, String, Option[String])] = {
      Some(d.email, d.password, d.returnUrl)
    }
  }

  val loginForm: Form[LoginData] = Form(
    mapping(
      "email" -> nonEmptyText,
      "password" -> nonEmptyText,
      "return_url" -> optional(text)
    )(LoginData.apply)(LoginData.unapply)
  )

  case class RegisterData(name: Option[String], email: String, password: String, passwordVerify: String, returnUrl: Option[String])
  object RegisterData {
    def unapply(d: RegisterData): Option[(Option[String], String, String, String, Option[String])] = {
      Some((d.name, d.email, d.password, d.passwordVerify, d.returnUrl))
    }
  }
  val registerForm: Form[RegisterData] = Form(
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
  object ForgotPasswordData {
    def unapply(d: ForgotPasswordData): Option[String] = Some(d.email)
  }
  private val forgotPasswordForm: Form[ForgotPasswordData] = Form(
    mapping(
      "email" -> nonEmptyText
    )(ForgotPasswordData.apply)(ForgotPasswordData.unapply)
  )

  case class ResetPasswordData(password: String, passwordVerify: String)
  object ResetPasswordData {
    def unapply(d: ResetPasswordData): Option[(String, String)] = Some((d.password, d.passwordVerify))
  }

  private val resetPasswordForm: Form[ResetPasswordData] = Form(
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
