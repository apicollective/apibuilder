package builder.api_json.templates

import cats.implicits._
import cats.data.ValidatedNec
import io.apibuilder.api.json.v0.models.{ApiJson, Templates}
import io.apibuilder.api.json.v0.models.json._
import play.api.libs.json.{JsObject, Json}

object JsMerge {
  def merge(js: JsObject): ValidatedNec[String, JsObject] = {
    js.asOpt[ApiJson] match {
      case None => js.validNec // Let parent validate the structure
      case Some(apiJson) => {
        apiJson.templates match {
          case None => js.validNec
          case Some(t) => merge(apiJson, t).map { r => Json.toJson(r).asInstanceOf[JsObject] }
        }
      }
    }
  }

  def merge(js: ApiJson, templates: Templates): ValidatedNec[String, ApiJson] = {
    println(s"Merging templates: ${templates}")
    val modelMerge = ModelMerge(templates.models.getOrElse(Map.empty)).merge(
      ModelMergeData(
        models = js.models,
        interfaces = js.interfaces
      )
    )
    val resourceMerge = ResourceMerge(templates.resources.getOrElse(Map.empty)).merge(
      ResourceMergeData(resources = js.resources)
    )
    js.validNec
  }
}