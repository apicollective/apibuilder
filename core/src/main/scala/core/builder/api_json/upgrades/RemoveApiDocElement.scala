package builder.api_json.upgrades

import play.api.libs.json.JsObject

/**
 * In Oct 2022 we removed support for the top level 'apidoc' node which allowed
 * the user to specify the version of API Builder. This feature was never
 * implemented.
 */
object RemoveApiDocElement extends Upgrader {
  def apply(js: JsObject): JsObject = {
    js - "apidoc"
  }
}