package builder.api_json.upgrades

import play.api.libs.json.JsValue

trait Upgrader {
  def apply(js: JsValue): JsValue
}

object AllUpgrades {

  private[this] val all: Seq[Upgrader] = List(ApiDocToApiBuilder, InterfacesToSupportResources)

  def apply(js: JsValue): JsValue = {
    all.foldLeft(js) { case (j, upgrader) =>
      upgrader.apply(j)
    }
  }

}
