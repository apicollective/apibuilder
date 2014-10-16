package core.generator

case class CodeGenTarget(key: String, name: String, description: Option[String], status: Status, generator: Option[CodeGenerator])

