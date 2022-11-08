package builder.api_json.templates

import cats.data.ValidatedNec
import cats.implicits._
import io.apibuilder.api.json.v0.models._

case class ModelMergeData(
  interfaces: Map[String, Interface],
  models: Map[String, Model]
)

case class ModelMerge(templates: Map[String, Model]) extends TemplateMerge[Model](templates) with AttributeMerge {

  def merge(data: ModelMergeData): ValidatedNec[String, ModelMergeData] = {
    (
      validateTemplateDeclarations(data.models),
      applyTemplates(data)
    ).mapN { case (_, models) =>
      ModelMergeData(
        models = models,
        interfaces = data.interfaces ++ buildInterfaces(data.interfaces)
      )
    }
  }

  private[this] def validateTemplateDeclarations(models: Map[String, Model]): ValidatedNec[String, Unit] = {
    models
      .flatMap { case (name, model) =>
      model.templates.getOrElse(Nil).map { tpl =>
        validateTemplateDeclaration(name, tpl)
      }
    }.toSeq.sequence.map(_ => ())
  }

  private[this] def validateTemplateDeclaration(modelName: String, decl: TemplateDeclaration): ValidatedNec[String, Unit] = {
    templates.get(decl.name) match {
      case None => s"Model[$modelName] cannot find template named '${decl.name}'".invalidNec
      case Some(_) => ().validNec
    }
  }

  private[this] def applyTemplates(data: ModelMergeData): ValidatedNec[String, Map[String, Model]] = {
    data.models.map { case (name, model) =>
      resolveTemplateDeclarations(model.templates).map { all =>
        name -> applyTemplates(name, model, all)
      }
    }.toSeq.sequence.map { all =>
      all.map { case (n, m) => n -> m }.toMap
    }
  }

  override def templateDeclarations(model: Model): Seq[TemplateDeclaration] = {
    model.templates.getOrElse(Nil)
  }

  override def applyTemplate(name: String, original: Model, tplName: String, tpl: Model): Model = {
    val templates = mergeTemplates(original.templates, tpl.templates)
    Model(
      plural = original.plural,
      description = original.description.orElse(tpl.description),
      deprecation = original.deprecation.orElse(tpl.deprecation),
      fields = mergeFields(original, tpl),
      attributes = mergeAttributes(original.attributes, tpl.attributes),
      templates = None,
      interfaces = union(original.interfaces.getOrElse(Nil), tpl.interfaces.getOrElse(Nil), templates.getOrElse(Nil).map(_.name))
    )
  }

  private[this] def buildInterfaces(defined: Map[String, Interface]): Map[String, Interface] = {
    templates.filterNot { case (name, _) => defined.contains(name) }.map { case (name, t) =>
      name -> Interface(
        plural = t.plural,
        description = t.description,
        deprecation = t.deprecation,
        fields = Some(t.fields),
        attributes = t.attributes,
      )
    }
  }

  def mergeTemplates(original: Option[Seq[TemplateDeclaration]], template: Option[Seq[TemplateDeclaration]]): Option[Seq[TemplateDeclaration]] = {
    new ArrayMerge[TemplateDeclaration]() {
      override def uniqueIdentifier(i: TemplateDeclaration): String = i.name

      override def merge(original: TemplateDeclaration, tpl: TemplateDeclaration): TemplateDeclaration = {
        TemplateDeclaration(
          name = original.name
        )
      }
    }.merge(original, template)
  }

  private[this] def mergeFields(model: Model, tpl: Model): Seq[Field] = {
    new ArrayMerge[Field] {
      override def uniqueIdentifier(f: Field): String = f.name

      override def merge(original: Field, tpl: Field): Field = {
        Field(
          name = original.name,
          `type` = original.`type`,
          description = original.description.orElse(tpl.description),
          deprecation = original.deprecation.orElse(tpl.deprecation),
          default = original.default.orElse(tpl.default),
          example = original.example.orElse(tpl.example),
          minimum = original.minimum.orElse(tpl.minimum),
          maximum = original.maximum.orElse(tpl.maximum),
          attributes = mergeAttributes(original.attributes, tpl.attributes),
          annotations = union(original.annotations.getOrElse(Nil), tpl.annotations.getOrElse(Nil))
        )
      }
    }.merge(model.fields, tpl.fields)
  }

}