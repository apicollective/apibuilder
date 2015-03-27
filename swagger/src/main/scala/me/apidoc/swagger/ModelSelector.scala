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
    remaining().headOption.map { md =>
      completed += md.name
      md
    }
  }

}



