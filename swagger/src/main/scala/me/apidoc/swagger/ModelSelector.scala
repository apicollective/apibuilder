package me.apidoc.swagger

import com.wordnik.swagger.{models => swagger}
import com.wordnik.swagger.models.properties.{ArrayProperty, Property, RefProperty}

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
        Util.toArray(m.getAllOf).flatMap { modelDependencies(_) }
      }

      case m: swagger.RefModel => {
        Seq(m.getSimpleRef)
      }

      case m: swagger.ModelImpl => {
        Util.toMap(m.getProperties).values.flatMap { schemaType(_) }.toSeq
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

  private var completed = scala.collection.mutable.ListBuffer[String]()

  def remaining(): Seq[MyDefinition] = {
    definitions.filter( md => !completed.contains(md.name) )
  }

  def next(): Option[MyDefinition] = {
    remaining().find { m =>
      m.dependencies.find( depName => !completed.contains(depName) ) match {
        case None => true
        case Some(_) => false
      }
    }.map { md =>
      completed += md.name
      md
    }
  }

}
