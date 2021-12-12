package io.apibuilder.swagger.v2

import cats.data.ValidatedNec
import cats.implicits._
import io.apibuilder.spec.v0.models._
import io.swagger.v3.oas.models.{OpenAPI, info}
import io.swagger.v3.oas.{models => swagger}
import scala.jdk.CollectionConverters._

case class ReferenceType[T](ref: String, value: T)

object Components {
  def validate(api: OpenAPI): ValidatedNec[String, Components] = {
    Option(api.getComponents) match {
      case None => Components(
        models = Nil
      ).validNec
      case Some(c) => {
        validateSchemas(c).map { models =>
          Components(
            models = models,
          )
        }
      }
    }
  }

  private[this] def validateSchemas(components: swagger.Components): ValidatedNec[String, Seq[ReferenceType[Model]]] = {
    Option(components.getSchemas).map(_.asScala).getOrElse(Nil).map { case (ref, schema) =>
      validateSchema(schema)
    }.toList.sequence
  }

  private[this] def validateSchema[T](schema: swagger.media.Schema[T]): ValidatedNec[String, ReferenceType[Model]] = {

  }
}

case class Components(models: Seq[ReferenceType[Model]]) {
  private[this] val modelsByRef: Map[String, Model] = models.map { m => m.ref -> m.value }.toMap

  def findModel(ref: String): Option[Model] = modelsByRef.get(ref)
}

case class ComponentsBuilder() {

}
