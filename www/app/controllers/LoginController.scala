package controllers

import client.ApidocClient

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.openid.{ Errors, OpenID, OpenIDError }

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

object LoginController extends Controller {

  private lazy val masterClient = ApidocClient.instance("f3973f60-be9f-11e3-b1b6-0800200c9a66")

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.Future

  def redirect = Action {
    Redirect(routes.LoginController.index())
  }

  def index = Action.async { implicit request =>
    for {
      user <- masterClient.users.findByEmail("admin@apidoc.me")
    } yield {
      Redirect("/").withSession { "user_guid" -> user.get.guid }
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
          val fullname = info.attributes.get("fullname")
          val imageUrl = info.attributes.get("image_url")

          val user = Await.result(masterClient.users.findByEmail(email), 1500.millis).getOrElse {
            Await.result(masterClient.users.create(email, fullname, imageUrl), 1500.millis)
          }
          Redirect("/").withSession { "user_guid" -> user.guid }
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
