package helpers

import io.apibuilder.spec.v0.models.Service
import io.apibuilder.spec.v0.models.json._
import play.api.libs.json.{JsObject, Json}

trait ApiJsonHelpers extends ServiceHelpers {

  def toApiJson(service: Service): JsObject = {
    Json.toJson(service).asInstanceOf[JsObject]
  }

}
