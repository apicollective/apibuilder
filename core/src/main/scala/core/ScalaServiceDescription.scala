package core

import Text._

object ScalaUtil {

  // TODO: Use GeneratorUtil.formatComment here
  def textToComment(text: String) = {
    if (text.isEmpty) {
      text
    } else {
      val lines = text.split("\n")
      lines.mkString("/**\n * ", "\n * ", "\n */\n")
    }
  }

  def fieldsToArgList(fields: Seq[String]) = {
    fields.map(_.indent).mkString("\n", ",\n", "\n")
  }
}

class ScalaServiceDescription(serviceDescription: ServiceDescription) {

  val name = safeName(serviceDescription.name)

  val description = serviceDescription.description.map(ScalaUtil.textToComment(_)).getOrElse("")

  val models = serviceDescription.models.map { new ScalaModel(_) }

  val resources = serviceDescription.resources.map { new ScalaResource(_) }

}

class ScalaModel(model: Model) {

  val name = underscoreToInitCap(model.name)

  val plural = underscoreToInitCap(model.plural)

  val description = model.description.getOrElse("")

  def scaladoc: String = {
    val base: String = description
    val fielddoc: List[String] = fields.toList.map { field =>
      s"@param ${field.name} ${field.description}"
    }
    ScalaUtil.textToComment((base :: fielddoc).mkString("\n"))
  }

  val fields = model.fields.map { new ScalaField(_) }

  val argList = ScalaUtil.fieldsToArgList(fields.map(_.definition))

}

class ScalaResource(resource: Resource) {
  val model = new ScalaModel(resource.model)

  val path = resource.path

  val operations = resource.operations.map { new ScalaOperation(_) }
}

class ScalaOperation(operation: Operation) {

  val method: String = operation.method

  val path: String = operation.path

  def scaladoc: String = {
    val base: String = operation.description.getOrElse("")
    val fielddoc: List[String] = parameters.toList.map { param =>
      s"@param ${param.name} ${param.description}"
    }
    ScalaUtil.textToComment((base :: fielddoc).mkString("\n"))
  }

  val parameters = operation.parameters.map { new ScalaParameter(_) }

  val name: String = {
    val pathParams = parameters.filter { p =>
      p.location == ParameterLocation.Path
    }
    val names = pathParams.map { p =>
      p.name.capitalize
    }
    val base = method.toLowerCase
    if (names.isEmpty) {
      base
    } else {
      base + "By" + names.mkString("And")
    }
  }

  val argList: String = ScalaUtil.fieldsToArgList(parameters.map(_.definition))

  val responses = operation.responses.map { new ScalaResponse(_) }
}

class ScalaResponse(response: Response) {
  def code = response.code

  def datatype = underscoreToInitCap(response.datatype)

  def multiple = response.multiple
}

// TODO support multiple
class ScalaField(field: Field) {

  def name: String = snakeToCamelCase(field.name)

  def originalName: String = field.name

  def datatype: ScalaDataType = field.fieldtype match {

    case t: PrimitiveFieldType => ScalaDataType(t.datatype)
    case m: ModelFieldType => new ScalaDataType(underscoreToInitCap(m.model.name))
    case r: ReferenceFieldType => new ScalaDataType(underscoreToInitCap(s"Reference[#{underscoreToInitCap(r.model.name)}]"))

  }

  def description: String = field.description.getOrElse("")

  def isOption: Boolean = !field.required || field.default.nonEmpty

  def multiple: Boolean = field.multiple

  def typeName: String = {
    if (multiple) {
      s"List[${datatype.name}]"
    } else if (isOption) {
      s"Option[${datatype.name}]"
    } else {
      datatype.name
    }
  }

  def definition: String = {
    val decl = s"$name: $typeName"
    if (multiple) {
      if (isOption) {
        decl + " = Nil"
      } else {
        decl
      }
    } else if (isOption) {
      decl + " = None"
    } else {
      decl
    }
  }
}

// TODO support multiple
class ScalaParameter(param: Parameter) {

  def name: String = snakeToCamelCase(param.name)

  def originalName: String = param.name

  def datatype: ScalaDataType = param.paramtype match {

    case t: PrimitiveParameterType => ScalaDataType(t.datatype)
    case m: ModelParameterType => new ScalaDataType(underscoreToInitCap(m.model.name))

  }

  def description: String = param.description.getOrElse("")

  def isOption: Boolean = !param.required || param.default.nonEmpty

  def multiple: Boolean = param.multiple

  def typeName: String = {
    if (multiple) {
      s"List[${datatype.name}]"
    } else if (isOption) {
      s"Option[${datatype.name}]"
    } else {
      datatype.name
    }
  }

  def definition: String = {
    val decl = s"$name: $typeName"
    if (multiple) {
      if (isOption) {
        decl + " = Nil"
      } else {
        decl
      }
    } else if (isOption) {
      decl + " = None"
    } else {
      decl
    }
  }

  def location = param.location
}

class ScalaDataType(val name: String)

object ScalaDataType {

  case object ScalaStringType extends ScalaDataType("java.lang.String")
  case object ScalaIntegerType extends ScalaDataType("scala.Int")
  case object ScalaLongType extends ScalaDataType("scala.Long")
  case object ScalaBooleanType extends ScalaDataType("scala.Boolean")
  case object ScalaDecimalType extends ScalaDataType("scala.BigDecimal")
  case object ScalaUnitType extends ScalaDataType("scala.Unit")
  case object ScalaUuidType extends ScalaDataType("java.util.UUID")
  case object ScalaDateTimeIso8601Type extends ScalaDataType("org.joda.time.DateTime")
  case object ScalaMoneyIso4217Type extends ScalaDataType("Money")

  def apply(datatype: Datatype): ScalaDataType = datatype match {
    case Datatype.StringType => ScalaStringType
    case Datatype.IntegerType => ScalaIntegerType
    case Datatype.LongType => ScalaLongType
    case Datatype.BooleanType => ScalaBooleanType
    case Datatype.DecimalType => ScalaDecimalType
    case Datatype.UnitType => ScalaUnitType
    case Datatype.UuidType => ScalaUuidType
    case Datatype.DateTimeIso8601Type => ScalaDateTimeIso8601Type
    case Datatype.MoneyIso4217Type => ScalaMoneyIso4217Type
  }

}
