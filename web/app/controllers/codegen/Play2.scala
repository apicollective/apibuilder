package controllers.codegen

import controllers.Authenticated
import models.MainTemplate
import core.ServiceDescription
import client.Apidoc
import client.Apidoc.{ Organization, Service, User, Version }
import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.Await
import scala.concurrent.duration._
import lib.RouteGenerator

import play.api._
import play.api.mvc._

object Play2 extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def routes(orgKey: String, serviceKey: String, versionName: String) = Authenticated.async { request =>
    for {
      org <- Apidoc.organizations.findByKey(orgKey)
      service <- Apidoc.services.findByOrganizationKeyAndKey(orgKey, serviceKey)
      versions <- Apidoc.versions.findAllByOrganizationKeyAndServiceKey(orgKey, serviceKey, 10)
      version <- Apidoc.versions.findByOrganizationKeyAndServiceKeyAndVersion(orgKey, serviceKey, versionName)
    } yield {
      val generator = RouteGenerator(version.json.get)
      Ok(generator.generate())
    }
  }

}
