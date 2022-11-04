package builder.api_json.templates

import cats.implicits._
import cats.data.ValidatedNec
import io.apibuilder.api.json.v0.models._

case class ResourceMergeData(resources: Map[String, Resource])
case class ResourceMerge(templates: Map[String, Resource]) extends TemplateMerge[Resource](templates) with HeaderMerge {

  def merge(data: ResourceMergeData): ValidatedNec[String, ResourceMergeData] = {
    data.resources.map { case (name, resource) =>
      allTemplates(resource.templates).map { all =>
        name -> applyTemplates(name, resource, all)
      }
    }.toSeq.sequence.map { all =>
      println(s"Merged resources. Original: ${data.resources}")
      println(s"  ==> ${all.map { case (n, m) => n -> m }.toMap}")
      ResourceMergeData(
        resources = all.map { case (n, m) => n -> m }.toMap
      )
    }
  }

  override def templateDeclarations(resource: Resource): Seq[TemplateDeclaration] = {
    resource.templates.getOrElse(Nil)
  }

  override def applyTemplate(originalName: String, original: Resource, tplName: String, tpl: Resource): Resource = {
    println(s"ResourceMerge.applyTemplate $originalName ==> $tplName")
    Resource(
      path = original.path.orElse(tpl.path),
      description = original.description.orElse(tpl.description),
      deprecation = original.deprecation.orElse(tpl.deprecation),
      operations = mergeOperations(original.operations, tpl.operations).map { op =>
        RenameTypes(tplName, originalName).rename(op)
      },
      attributes = mergeAttributes(original.attributes, tpl.attributes),
      templates = None,
    )
  }

  private[this] def pathLabel(op: Operation): String = {
    (op.method + ":" + op.path).toLowerCase().trim
  }

  private[this] def mergeOperations(original: Seq[Operation], template: Seq[Operation]): Seq[Operation] = {
    new ArrayMerge[Operation]() {
      override def uniqueIdentifier(i: Operation): String = pathLabel(i)
      override def merge(original: Operation, tpl: Operation): Operation = {
        Operation(
          method = original.method,
          path = original.path,
          description = original.description.orElse(tpl.description),
          deprecation = original.deprecation.orElse(tpl.deprecation),
          parameters = mergeParameters(original.parameters, tpl.parameters),
          body = original.body.orElse(tpl.body),
          responses = mergeResponses(original.responses, tpl.responses),
          attributes = mergeAttributes(original.attributes, tpl.attributes)
        )
      }
    }.merge(original, template)
  }

  private[this] def mergeParameters(original: Option[Seq[Parameter]], template: Option[Seq[Parameter]]): Option[Seq[Parameter]] = {
    new ArrayMerge[Parameter]() {
      override def uniqueIdentifier(i: Parameter): String = i.name

      override def merge(original: Parameter, tpl: Parameter): Parameter = {
        Parameter(
          name = original.name,
          `type` = original.`type`,
          location = original.location,
          description = original.description.orElse(tpl.description),
          deprecation = original.deprecation.orElse(tpl.deprecation),
          required = original.required,
          default = original.default.orElse(tpl.default),
          minimum = original.minimum.orElse(tpl.minimum),
          maximum = original.maximum.orElse(tpl.maximum),
          example = original.example.orElse(tpl.example),
          attributes = mergeAttributes(original.attributes, tpl.attributes)
        )
      }
    }.merge(original, template)
  }

  private[this] def mergeResponses(original: Option[Map[String, Response]], template: Option[Map[String, Response]]): Option[Map[String, Response]] = {
    new MapMerge[Response]() {
      override def merge(original: Response, tpl: Response): Response = {
        println(s"mergeResponses original: $original tpl: $tpl")
        Response(
          `type` = original.`type`,
          headers = mergeHeaders(original.headers, tpl.headers),
          description = original.description.orElse(tpl.description),
          deprecation = original.deprecation.orElse(tpl.deprecation),
          attributes = mergeAttributes(original.attributes, tpl.attributes)
        )
      }
    }.merge(original, template)
  }

}