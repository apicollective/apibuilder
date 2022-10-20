package builder.api_json.upgrades

import play.api.libs.json.{JsObject, JsValue}

trait Upgrader {
  final def apply(js: JsValue): JsValue = {
    js match {
      case o: JsObject => apply(o)
      case j => j
    }
  }

  def apply(js: JsObject): JsObject
}

object AllUpgrades {

  private[this] val all: Seq[Upgrader] = List(RemoveApiDocElement, InterfacesToSupportResources)

  def apply(js: JsValue): JsValue = {
    all.foldLeft(js) { case (j, upgrader) =>
      upgrader.apply(j)
    }
  }

}
