package io.apibuilder.swagger.v2

import cats.data.ValidatedNec
import cats.implicits._
import io.apibuilder.spec.v0.models._
import io.swagger.v3.oas.models.{OpenAPI, PathItem, Paths}

import scala.jdk.CollectionConverters._

object ResourcesValidator extends OpenAPIParseHelpers {
  def validate(api: OpenAPI, components: Components): ValidatedNec[String, Seq[Resource]] = {
    Option(api.getPaths) match {
      case None => Nil.validNec
      case Some(c) => validateResources(c.asScala.toMap)
    }
  }

  private[this] def validateResources(components: Components, paths: Map[String, PathItem]): ValidatedNec[String, Seq[Resource]] = {
    paths.keys.toList.sortBy(_.length).map { url =>
      val pathItem = paths(url)

      validateOperations(components, pathItem).map { operations =>

        println(s" URL - $url: ${pathItem}")
        Resource(
          `type`: String,
          plural: String,
          path = Some(url),
          description = trimmedString(pathItem.getDescription),
          deprecation = None,
          attributes = Nil, // Not supported
          operations = operation,
        )
      }
    }.traverse(identity)
  }

  private[this] def validateOperations(components: Components, item: PathItem): ValidatedNec[String, Operation] = {
    TODO: Map all the operations
    Seq(
      Option(item.getGet),
      Option(item.),
      Option(item.),
      Option(item.),
      Option(item.),
      Option(item.),
      Option(item.),
      Option(item.),
    )
    Operation(
      method: io.apibuilder.spec.v0.models.Method,
      path: String,
      description: _root_.scala.Option[String] = None,
    deprecation: _root_.scala.Option[io.apibuilder.spec.v0.models.Deprecation] = None,
    body: _root_.scala.Option[io.apibuilder.spec.v0.models.Body] = None,
    parameters: Seq[io.apibuilder.spec.v0.models.Parameter] = Nil,
    responses: Seq[io.apibuilder.spec.v0.models.Response] = Nil,
    attributes: Seq[io.apibuilder.spec.v0.models.Attribute] = Nil

    )
  }
}
