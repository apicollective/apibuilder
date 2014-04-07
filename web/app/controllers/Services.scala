package controllers

import models.MainTemplate
import Apidoc.Version
import core.{ Organization, Service }
import play.api._
import play.api.mvc._
import scala.concurrent.Await
import scala.concurrent.duration._

object Services extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def resource(orgKey: String, serviceKey: String, version: String, resourceKey: String) = TODO

}
