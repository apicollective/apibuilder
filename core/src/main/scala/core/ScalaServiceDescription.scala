package core

import Text._

object ScalaUtil {

  // TODO: Use GeneratorUtil.formatComment here
  def textToComment(text: String) = {
    val lines = text.split("\n")
    lines.mkString("/**\n * ", "\n * ", "\n */\n")
  }

  def fieldsToArgList(fields: Seq[String]) = {
    fields.map(_.indent).mkString("\n", ",\n", "\n")
  }
}

class ScalaServiceDescription(serviceDescription: ServiceDescription) {

  val name = safeName(serviceDescription.name)

  val description = serviceDescription.description.map(ScalaUtil.textToComment(_)).getOrElse("")

  val models = serviceDescription.models.map { new ScalaModel(_) }

  val operations = serviceDescription.operations.map { new ScalaOperation(_) }

}

class ScalaModel(model: Model) {

  val name = underscoreToInitCap(model.name)

  val description = model.description.map(ScalaUtil.textToComment).getOrElse("")

  val fields = model.fields.map { new ScalaField(_) }

  val argList = ScalaUtil.fieldsToArgList(fields.map(_.src))

}

class ScalaOperation(operation: Operation) {

  val method = operation.method

  val path = operation.path

  val description = operation.description.map(ScalaUtil.textToComment).getOrElse("")

  val parameters = operation.parameters.map { new ScalaParameter(_) }.sorted

  val name = method.toLowerCase + safeName(path).capitalize

  val argList = ScalaUtil.fieldsToArgList(parameters.map(_.src))

}

class ScalaField(field: Field) extends Source with Ordered[ScalaField] {

  def name: String = snakeToCamelCase(field.name)

  def originalName: String = field.name

  def datatype: ScalaDataType = ScalaDataType(field.datatype)

  def description: String = field.description.map(ScalaUtil.textToComment).getOrElse("")

  def isOption: Boolean = !field.required || field.default.nonEmpty

  def typeName: String = if (isOption) s"Option[${datatype.name}]" else datatype.name

  // TODO: RENAME
  override def src: String = {
    val decl = s"$description$name: $typeName"
    if (isOption) decl + " = None" else decl
  }

  // we just want to make sure that fields with defaults
  // always come after those without, so that argument lists will
  // be valid. otherwise, preserve existing order
  override def compare(that: ScalaField): Int = {
    if (isOption) {
      if (that.isOption) {
        0
      } else {
        1
      }
    } else if (that.isOption) {
      -1
    } else {
      0
    }
  }
}

class ScalaParameter(param: Parameter) extends Source with Ordered[ScalaParameter] {

  def name: String = snakeToCamelCase(param.name)

  def originalName: String = param.name

  def datatype: ScalaDataType = param.paramtype match {

    case t: PrimitiveParameterType => ScalaDataType(t.datatype)
    case m: ModelParameterType => new ScalaDataType(underscoreToInitCap(m.model.name))

  }

  def description: String = param.description.map(ScalaUtil.textToComment).getOrElse("")

  def isOption: Boolean = !param.required || param.default.nonEmpty

  def typeName: String = if (isOption) s"Option[${datatype.name}]" else datatype.name

  // TODO: RENAME
  override def src: String = {
    val decl = s"$description$name: $typeName"
    if (isOption) decl + " = None" else decl
  }

  // we just want to make sure that params with defaults
  // always come after those without, so that argument lists will
  // be valid. otherwise, preserve existing order
  override def compare(that: ScalaParameter): Int = {
    if (isOption) {
      if (that.isOption) {
        0
      } else {
        1
      }
    } else if (that.isOption) {
      -1
    } else {
      0
    }
  }
}

class ScalaDataType(val name: String) extends Source {

  // TODO: Remove this and just access datatype directly
  override val src: String = name

}

object ScalaDataType {

  def apply(datatype: Datatype): ScalaDataType = {
    val name = datatype match {
      case Datatype.String => "String"
      case Datatype.Integer => "Int"
      case Datatype.Long => "Long"
      case Datatype.Boolean => "Boolean"
      case Datatype.Decimal => "BigDecimal"
      case Datatype.Unit => "Unit"
      case Datatype.Uuid => "UUID"
      case Datatype.DateTimeIso8601 => "DateTime"
    }
    new ScalaDataType(name)
  }

}
