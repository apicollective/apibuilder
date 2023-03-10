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
        interfaces = data.interfaces ++ buildInterfaces(data, models.map { case (n,m) => ModelWithName(n, m) }.toSeq)
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

  override def applyTemplate(original: Model, tpl: Model): Model = {
    val templates = mergeTemplates(original.templates, tpl.templates)
    Model(
      plural = original.plural,
      description = original.description.orElse(tpl.description),
      deprecation = original.deprecation.orElse(tpl.deprecation),
      fields = mergeFields(original, tpl),
      attributes = mergeAttributes(original.attributes, tpl.attributes),
      templates = original.templates,
      interfaces = union(original.interfaces.getOrElse(Nil), tpl.interfaces.getOrElse(Nil), templates.getOrElse(Nil).map(_.name))
    )
  }

  private[this] case class ModelWithName(name: String, model: Model) {
    val fields: Seq[Field] = model.fields
  }

  private[this] def buildInterfaces(data: ModelMergeData, models: Seq[ModelWithName]): Map[String, Interface] = {
    templates.filterNot { case (name, _) => data.interfaces.contains(name) }.map { case (name, t) =>
      name -> Interface(
        plural = t.plural,
        description = t.description,
        deprecation = t.deprecation,
        fields = Some(fieldsWithSameInterface(selectModelsDeclaringTemplate(models, name), t.fields)),
        attributes = t.attributes,
      )
    }
  }

  private[this] def selectModelsDeclaringTemplate(models: Seq[ModelWithName], templateName: String): Seq[ModelWithName] = {
    models.filter { m =>
      m.model.templates.getOrElse(Nil).map(_.name).contains(templateName)
    }
  }

  private[this] def info(f: Field): String = {
    s"${f.name}: ${f.`type`} " + (f.required match {
      case true => "not null"
      case false => "null"
    })
  }

  private[this] def fieldsWithSameInterface(models: Seq[ModelWithName], fields: Seq[Field]): Seq[Field] = {
    fields.filter { f =>
      models.forall(hasFieldWithSameInterface(_, f))
    }
  }

  private[this] def hasFieldWithSameInterface(model: ModelWithName, field: Field): Boolean = {
    model.fields.find(_.name == field.name) match {
      case None => false
      case Some(f) => hasSameInterface(f, field)
    }
  }

  private[this] def hasSameInterface(f1: Field, f2: Field): Boolean = {
    f1.name == f2.name && f1.`type` == f2.`type` && f1.required == f2.required
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