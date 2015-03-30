package me.apidoc.swagger

import com.wordnik.swagger.{models => swagger}

private[swagger] case class MyDefinition(name: String, definition: swagger.Model) {


  /**
    * the list of types that this definition depends on
    */
  lazy val dependencies: Seq[String] = modelDependencies(definition)

  private def modelDependencies(swaggerModel: swagger.Model): Seq[String] = {
    swaggerModel match {
      case m: swagger.ComposedModel => {
        Util.toArray(m.getAllOf).flatMap { modelDependencies(_) }
      }

      case m: swagger.RefModel => {
        Seq(m.getSimpleRef)
      }

      case m: swagger.ModelImpl => {
        // TODO: m.fields
        Nil
      }

      case _ => {
        Nil
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



