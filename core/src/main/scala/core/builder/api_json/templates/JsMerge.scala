package builder.api_json.templates

import cats.data.ValidatedNec
import cats.implicits._
import io.apibuilder.api.json.v0.models.json._
import io.apibuilder.api.json.v0.models.{ApiJson, Model, Templates}
import play.api.libs.json.{JsError, JsObject, JsSuccess, Json}

object JsMerge {
  def merge(js: JsObject): ValidatedNec[String, JsObject] = {
    js.validate[ApiJson] match {
      case e: JsError => {
        js.validNec
      } // Let parent validate the structure
      case JsSuccess(apiJson, _) => {
        apiJson.templates match {
          case None => js.validNec
          case Some(t) => merge(apiJson, t).map { r => Json.toJson(r).asInstanceOf[JsObject] }
        }
      }
    }
  }

  def merge(js: ApiJson, templates: Templates): ValidatedNec[String, ApiJson] = {
    mergeTemplates(js, templates).map { apiJson =>
      // Note we build types from the original js as we remove templates after merging
      RenameTypes(typesToRename(js)).rename(apiJson)
    }
  }

  /**
   * In a type declaration, we have data that looks like:
   *   - template name: 'group'
   *   - resource "user_group" using template "group"
   *   - we user_group is the name of the model, we need to rewrite the return type
   *     of 'group' to the type of the resource 'user_group'
   * This method collects the mappings from the template name to the actual model|resource type
   */
  private[this] def typesToRename(json: ApiJson): Seq[Renaming] = {
    json.models
      .flatMap { case (modelName, model) =>
      model.templates.getOrElse(Nil).map(_.name).map { tplName => Renaming(tplName, modelName) }
    }.toSeq ++ json.resources.flatMap { case (resourceType, resource) =>
      resource.templates.getOrElse(Nil).map(_.name).map { tplName => Renaming(tplName, resourceType) }
    }.toSeq
  }

  private[this] def mergeTemplates(js: ApiJson, templates: Templates): ValidatedNec[String, ApiJson] = {
    (
      validateTemplateNames(js, templates),
      mergeModels(js, templates),
      mergeResources(js, templates)
    ).mapN { case (_, modelMerge, resourceMerge) =>
      js.copy(
        models = modelMerge.models,
        interfaces = modelMerge.interfaces,
        resources = resourceMerge.resources,
        templates = None
      )
    }
  }

  private[this] def mergeModels(js: ApiJson, templates: Templates): ValidatedNec[String, ModelMergeData] = {
    ModelMerge(templates.models.getOrElse(Map.empty)).merge(
      ModelMergeData(
        models = js.models,
        interfaces = js.interfaces
      )
    )
  }

  private[this] def mergeResources(js: ApiJson, templates: Templates): ValidatedNec[String, ResourceMergeData] = {
    ResourceMerge(templates.resources.getOrElse(Map.empty)).merge(
      ResourceMergeData(resources = js.resources)
    )
  }

  private[this] def validateTemplateNames(js: ApiJson, templates: Templates): ValidatedNec[String, Unit] = {
    (
      validateTemplateNamesModel(templates.models.getOrElse(Map.empty), js.models.keys.toSet),
      validateTemplateNamesInterface(templates.models.getOrElse(Map.empty), js.interfaces.keys.toSet)
    ).mapN { case (_, _) => () }
  }

  private[this] def validateTemplateNamesModel(templateModels: Map[String, Model], names: Set[String]): ValidatedNec[String, Unit] = {
    templateModels.keys.filter(names.contains).toList match {
      case Nil => ().validNec
      case dups => dups.map { n =>
        s"Name[$n] cannot be used as the name of both a model and a template model"
      }.mkString(", ").invalidNec
    }
  }

  private[this] def validateTemplateNamesInterface(templateModels: Map[String, Model], names: Set[String]): ValidatedNec[String, Unit] = {
    templateModels.keys.filter(names.contains).toList match {
      case Nil => ().validNec
      case dups => dups.map { n =>
        s"Name[$n] cannot be used as the name of both an interface and a template model"
      }.mkString(", ").invalidNec
    }
  }
}