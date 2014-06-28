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
    Ok(views.html.login.index(loginForm))
  }

  def indexPost = Action.async { implicit request =>
    val form = loginForm.bindFromRequest
    form.fold (

      formWithErrors => Future {
        Ok(views.html.login.index(formWithErrors))
      },

      validForm => {
        Authenticated.api.Users.postAuthenticate(email = validForm.email, password = validForm.password).map { r =>
          val user = r.entity
          Redirect("/").withSession { "user_guid" -> user.guid.toString }
        }.recover {
          case apidoc.FailedResponse(errors: Seq[apidoc.models.Error], 409) => {
            Ok(views.html.login.index(form, Some(errors.map(_.message).mkString(", "))))
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

}
