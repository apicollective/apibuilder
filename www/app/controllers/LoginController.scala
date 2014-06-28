package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.openid.{ Errors, OpenID, OpenIDError }

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

object LoginController extends Controller {

  import scala.concurrent.ExecutionContext.Implicits.global

  def redirect = Action {
    Redirect(routes.LoginController.index())
  }

  def index = Action.async { implicit request =>
    val email = "admin@apidoc.me"
    for {
      userResponse <- Authenticated.api.Users.get(email = Some(email))
    } yield {
      val user = userResponse.entity.headOption.getOrElse {
        sys.error(s"Could not find user with email[$email]")
      }
      Redirect("/").withSession { "user_guid" -> user.guid.toString }
    }

  }


  def indexPost = Action.async { implicit request =>
    Form(single(
      "openid" -> nonEmptyText
    )).bindFromRequest.fold(
      error => {
        Logger.info("bad request " + error.toString)
        Future.successful { BadRequest(error.toString) }
      },
      {
        case (openid) => {
          OpenID.redirectURL(openid,
                             routes.LoginController.callback.absoluteURL(),
                             Seq("fullname" -> "http://axschema.org/namePerson",
                                 "email" -> "http://axschema.org/contact/email",
                                 "image" -> "http://axschema.org/media/image/default"))
          .map( url => Redirect(url) )
          .recover {
            case e: OpenIDError => {
              Redirect(routes.LoginController.index).flashing(
                "error" -> e.message
              )
            }
          }
        }
      }
    )
  }

  def callback = Action.async { implicit request =>
    OpenID.verifiedId.map(info => {

      info.attributes.get("email") match {
        case None => {
          Redirect(routes.LoginController.index).flashing(
            "error" -> "Open ID account did not provide your email address. Please try again or use a different OpenID"
          )
        }

        case Some(email: String) => {
          val user = Await.result(Authenticated.api.Users.get(email = Some(email)), 1500.millis).entity.headOption.getOrElse {
            Await.result(Authenticated.api.Users.post(email = email,
                                                      name = info.attributes.get("fullname"),
                                                      imageUrl = info.attributes.get("image_url")), 1500.millis).entity
          }
          Redirect("/").withSession { "user_guid" -> user.guid.toString }
        }
      }
    })

    .recover {
      case e: Throwable => {
        Logger.error("Error authenticating open id user", e)
        Redirect(routes.LoginController.index).flashing(
          "error" -> "Error authenticating. Please try again or provide a different OpenID URL"
        )
      }
    }

  }
}
