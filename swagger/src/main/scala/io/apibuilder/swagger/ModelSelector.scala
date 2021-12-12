package io.apibuilder.swagger

import io.swagger.{models => swagger}
import io.swagger.models.properties.{ArrayProperty, Property, RefProperty}

import scala.annotation.tailrec

private[swagger] case class MyDefinition(name: String, definition: swagger.Model) {

  /**
    * the list of types that this definition depends on
    */
  val dependencies: Seq[String] = modelDependencies(definition)

  /**
    * Returns a list of all the non primitive types that this model
    * depends on. Resolves references and inspects the properties of
    * all fields defined on this model.
    */
  private def modelDependencies(swaggerModel: swagger.Model): Seq[String] = {
    swaggerModel match {
      case m: swagger.ComposedModel => {
        Util.toArray(m.getAllOf).flatMap { modelDependencies }
      }

      case m: swagger.RefModel => {
        Seq(m.getSimpleRef)
      }

      case m: swagger.ModelImpl => {
        Util.toMap(m.getProperties).values.flatMap { schemaType }.toSeq
      }

      case _ => {
        Nil
      }
    }
  }

  /**
    * If the type of this property is a primitive, returns
    * None. Otherwise returns the name of the type.
    */
  @tailrec
  private def schemaType(prop: Property): Option[String] = {
    prop match {
      case p: ArrayProperty => {
        schemaType(p.getItems)
      }
      case p: RefProperty => {
        Some(p.getSimpleRef)
      }
      case _ => {
        SchemaType.fromSwagger(prop.getType, Option(prop.getFormat)) match {
          case None => Some(prop.getType)
          case Some(_) => None // Primitive type - no need to resolve
        }
      }
    }
  }

}

private[swagger] case class ModelSelector(
  swaggerDefinitions: Map[String, swagger.Model]
) {

  private val definitions = swaggerDefinitions.map {
    case (name, definition) => MyDefinition(name, definition)
  }.toSeq

  private val completed = scala.collection.mutable.ListBuffer[String]()

  def remaining(): Seq[MyDefinition] = {
    definitions.filter( md => !completed.contains(md.name) )
  }

  def next(): Option[MyDefinition] = {
    val remainingDefs = remaining()
    remainingDefs.find { m =>
      m.dependencies.forall( depName =>
          m.name == depName
          || completed.contains(depName)
          || remainingDefs.exists(_.dependencies.contains(depName)) //circular dependencies
      )
    }.map { md =>
      completed += md.name
      md
    }
  }

}
