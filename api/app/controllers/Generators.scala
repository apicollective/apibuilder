package controllers

import com.gilt.apidoc.models.json._
import lib.Validation
import play.api.mvc._
import play.api.libs.json._
import java.util.UUID

object Generators extends Controller {

  def get(orgKey: String) = TODO
  def getByGuid(orgKey: String, guid: java.util.UUID) = TODO
  def post(orgKey: String) = TODO
  def deleteByGuid(orgKey: String, guid: java.util.UUID) = TODO

}
