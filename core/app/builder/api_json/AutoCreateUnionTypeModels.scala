package builder.api_json

import cats.implicits.*
import lib.{DatatypeResolver, Text}

/**
 * Create default models for types in a union type if the user has not already created that
 * model. Works as syntactic sugar for defining a model as a type in a union.
 */
object AutoCreateUnionTypeModels {

  def createModelsForUnionTypes(resolver: DatatypeResolver, unions: Seq[InternalUnionForm]): Seq[InternalModelForm] = {
    unions.flatMap { u =>
      u.types.flatMap { t =>
        t.datatype.toOption.map(_.name).flatMap { typ =>
          if (isImported(typ)) {
            None
          } else {
            t.fields match {
              case None => None
              case Some(_) if resolver.parse(typ).isDefined => None
              case Some(fields) => Some(createModelForUnionType(typ, fields))
            }
          }
        }
      }
    }.distinctBy(_.name)
  }

  private def isImported(typ: String): Boolean = typ.indexOf(".") > 0

  private def createModelForUnionType(typ: String, fields: Seq[InternalFieldForm]): InternalModelForm = {
    InternalModelForm(
      name = typ,
      plural = Text.pluralize(typ),
      description = None,
      deprecation = None,
      fields = fields,
      attributes = Nil,
      interfaces = Nil,
      templates = Nil,
      warnings = ().validNec
    )
  }
}
