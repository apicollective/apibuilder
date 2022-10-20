package builder.api_json.upgrades

import play.api.libs.json.JsObject

object ApiDocRemovedFromSpec extends Upgrader {
  def apply(js: JsObject): JsObject = {
    js - "apidoc"
  }
}