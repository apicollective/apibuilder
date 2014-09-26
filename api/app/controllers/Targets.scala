package controllers

import com.gilt.apidoc.models.Target
import com.gilt.apidoc.models.json._
import core.generator.CodeGenTarget
import play.api.mvc._
import play.api.libs.json._

object Targets extends Controller {

  def getByOrgKey(orgKey: String) = AnonymousRequest { request =>
    val targets = CodeGenTarget.Implemented.filter(_.supportsOrganization(orgKey)).map(cgt =>
      Target(cgt.key, cgt.description)
    )
    Ok(Json.toJson(targets))
  }
}
