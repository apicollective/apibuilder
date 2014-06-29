package controllers

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

  def index() = Action { implicit request =>
    Ok(views.html.login.index(Tab.Login, loginForm, registerForm))
  }

  def indexPost = Action.async { implicit request =>
    val form = loginForm.bindFromRequest
    form.fold (

      formWithErrors => Future {
        Ok(views.html.login.index(Tab.Login, formWithErrors, registerForm))
      },

      validForm => {
        Authenticated.api.Users.postAuthenticate(email = validForm.email, password = validForm.password).map { r =>
          val user = r.entity
          Redirect("/").withSession { "user_guid" -> user.guid.toString }
        }.recover {
          case apidoc.FailedResponse(errors: Seq[apidoc.models.Error], 409) => {
            Ok(views.html.login.index(Tab.Login, form, registerForm, Some(errors.map(_.message).mkString(", "))))
          }
        }
      }

    )
  }

  def registerPost = Action.async { implicit request =>
    val form = registerForm.bindFromRequest
    form.fold (

      formWithErrors => Future {
        Ok(views.html.login.index(Tab.Register, loginForm, formWithErrors))
      },

      validForm => {
        Authenticated.api.Users.post(name = validForm.name, email = validForm.email, password = validForm.password).map { r =>
          val user = r.entity
          Redirect("/").withSession { "user_guid" -> user.guid.toString }
        }.recover {
          case apidoc.FailedResponse(errors: Seq[apidoc.models.Error], 409) => {
            Ok(views.html.login.index(Tab.Register, loginForm, form, Some(errors.map(_.message).mkString(", "))))
          }
        }
      }

    )
  }

  case class LoginData(email: String, password: String)
  val loginForm = Form(
    mapping(
      "email" -> nonEmptyText,
      "password" -> nonEmptyText
    )(LoginData.apply)(LoginData.unapply)
  )

  case class RegisterData(name: Option[String], email: String, password: String, password_verify: String)
  val registerForm = Form(
    mapping(
      "name" -> optional(text),
      "email" -> nonEmptyText,
      "password" -> nonEmptyText(minLength=5),
      "password_verify" -> nonEmptyText
    )(RegisterData.apply)(RegisterData.unapply) verifying("Password and password verify do not match", { f =>
      f.password == f.password_verify
    })
  )

  sealed trait Tab
  object Tab {

    case object Login extends Tab
    case object Register extends Tab

  }


}
