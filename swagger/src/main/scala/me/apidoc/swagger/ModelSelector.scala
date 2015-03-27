package me.apidoc.swagger

import com.wordnik.swagger.{models => swagger}

private[swagger] case class MyDefinition(name: String, definition: swagger.Model)

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
    remaining().find { m => !isComposedModel(m) } match {
      case Some(m) => {
        Some(select(m))
      }

      case None => {
        remaining().headOption.map( m => select(m) )
      }
    }
  }

  private def select(md: MyDefinition): MyDefinition = {
    completed += md.name
    md
  }

  private def isComposedModel(md: MyDefinition): Boolean = {
    md.definition match {
      case m: swagger.ComposedModel => true
      case _ => false
    }
  }

}



