package core.generator

import core._
import Text._

object ScalaUtil {

  private val Keywords = Seq("case", "catch", "class", "def", "do",
                             "else", "extends", "false", "final",
                             "finally", "for", "forSome", "if",
                             "implicit", "import", "lazy", "match",
                             "new", "null", "object", "override",
                             "package", "private", "protected",
                             "return", "sealed", "super", "this",
                             "throw", "trait", "try", "true",
                             "type", "val", "var", "while",
                             "with", "yeild").toSet

  def textToComment(text: String) = {
    if (text.trim.isEmpty) {
      ""
    } else {
      "/**\n * " + GeneratorUtil.splitIntoLines(text).mkString("\n * ") + "\n */"
    }
  }

  def fieldsToArgList(fields: Seq[String]) = {
    fields.map(_.indent).mkString("\n", ",\n", "\n")
  }

  def quoteNameIfKeyword(name: String): String = {
    if (Keywords.contains(name)) {
      "`" + name + "`"
    } else {
      name
    }
  }
}

class ScalaServiceDescription(serviceDescription: ServiceDescription) {

  val name = safeName(serviceDescription.name)

  val models = serviceDescription.models.map { new ScalaModel(_) }

  val resources = serviceDescription.resources.map { new ScalaResource(_) }

  val packageName = name.toLowerCase
}

class ScalaModel(model: Model) {

  val name: String = underscoreToInitCap(model.name)

  val plural: String = underscoreToInitCap(model.plural)

  val description: Option[String] = model.description

  val fields = model.fields.map { new ScalaField(_) }

  val argList = ScalaUtil.fieldsToArgList(fields.map(_.definition))

}

class ScalaResource(resource: Resource) {
  val model = new ScalaModel(resource.model)

  val path = resource.path

  val operations = resource.operations.map {
    new ScalaOperation(model, _, this)
  }
}

class ScalaOperation(model: ScalaModel, operation: Operation, resource: ScalaResource) {

  val method: String = operation.method

  val path: String = operation.path

  val description: Option[String] = operation.description

  val parameters: List[ScalaParameter] = {
    operation.parameters.toList.map { new ScalaParameter(_) }
  }

  lazy val pathParameters = parameters.filter { _.location == ParameterLocation.Path }

  lazy val queryParameters = parameters.filter { _.location == ParameterLocation.Query }

  lazy val name: String = GeneratorUtil.urlToMethodName(resource.path, operation.method, operation.path)

  lazy val body: Option[ScalaBody] = operation.body.map(new ScalaBody(_))

  lazy val argList: String = {
    val base = parameters.map(_.definition)
    ScalaUtil.fieldsToArgList(if (method != "GET") base :+ s"_body: ${body.get.model.name}" else base)
  }

  lazy val responses: List[ScalaResponse] = {
    operation.responses.toList.map { new ScalaResponse(_) }
  }

  lazy val resultType = responses.collectFirst {
    case r if r.isSuccess => r.datatype
  }.getOrElse("Unit")
}

class ScalaBody(body: Body) {
  lazy val model = new ScalaModel(body.model)
}

class ScalaResponse(response: Response) {
  def code = response.code

  def isSuccess = code >= 200 && code < 300

  def datatype = {
    val scalaName: String = underscoreToInitCap(response.datatype)
    if (response.multiple) {
      s"scala.collection.Seq[${scalaName}]"
    } else {
      scalaName
    }
  }

  def returndoc: String = s"($code, $datatype)"
}

class ScalaField(field: Field) {

  def name: String = ScalaUtil.quoteNameIfKeyword(snakeToCamelCase(field.name))

  def originalName: String = field.name

  def datatype: ScalaDataType = {
    import ScalaDataType._
    val base: ScalaDataType = field.fieldtype match {
      case t: PrimitiveFieldType => ScalaDataType(t.datatype)
      case m: ModelFieldType => new ScalaModelType(new ScalaModel(m.model))
      // TODO support references in scala
      case r: ReferenceFieldType => ???
    }
    if (multiple) {
      new ScalaListType(base)
    } else if (isOption) {
      new ScalaOptionType(base)
    } else {
      base
    }
  }

  def description: Option[String] = field.description

  def multiple: Boolean = field.multiple

  def typeName: String = datatype.name

  /**
   * If there is a default, ensure it is only set server side otherwise
   * changing the default would have no impact on deployed clients
   */
  def isOption: Boolean = !field.required || field.default.nonEmpty

  def definition: String = {
    val decl = s"$name: $typeName"
    if (isOption) {
      if (multiple) {
        decl + " = Nil"
      } else {
        decl + " = None"
      }
    } else {
      decl
    }
  }
}

class ScalaParameter(param: Parameter) {

  def name: String = ScalaUtil.quoteNameIfKeyword(snakeToCamelCase(param.name))

  def originalName: String = param.name

  def datatype: ScalaDataType = {
    import ScalaDataType._
    val base: ScalaDataType = param.paramtype match {
      case t: PrimitiveParameterType => ScalaDataType(t.datatype)
      case m: ModelParameterType => new ScalaModelType(new ScalaModel(m.model))
    }

    if (multiple) {
      new ScalaListType(base)
    } else if (isOption) {
      new ScalaOptionType(base)
    } else {
      base
    }
  }

  def description: String = param.description.getOrElse(name)

  def multiple: Boolean = param.multiple

  def typeName: String = datatype.name

  /**
   * If there is a default, ensure it is only set server side otherwise
   * changing the default would have no impact on deployed clients
   */
  def isOption: Boolean = !param.required || param.default.nonEmpty

  def definition: String = {
    val decl = s"$name: $typeName"
    if (isOption) {
      if (multiple) {
        decl + " = Nil"
      } else {
        decl + " = None"
      }
    } else {
      decl
    }
  }

  def location = param.location

}

sealed abstract class ScalaDataType(val name: String)

object ScalaDataType {

  case object ScalaStringType extends ScalaDataType("String")
  case object ScalaIntegerType extends ScalaDataType("Int")
  case object ScalaDoubleType extends ScalaDataType("Double")
  case object ScalaLongType extends ScalaDataType("Long")
  case object ScalaBooleanType extends ScalaDataType("Boolean")
  case object ScalaDecimalType extends ScalaDataType("BigDecimal")
  case object ScalaUnitType extends ScalaDataType("Unit")
  case object ScalaUuidType extends ScalaDataType("java.util.UUID")
  case object ScalaDateTimeIso8601Type extends ScalaDataType("org.joda.time.DateTime")
  case object ScalaMoneyIso4217Type extends ScalaDataType("Money")

  case class ScalaListType(inner: ScalaDataType) extends ScalaDataType(s"scala.collection.Seq[${inner.name}]")
  case class ScalaModelType(model: ScalaModel) extends ScalaDataType(model.name)
  case class ScalaOptionType(inner: ScalaDataType) extends ScalaDataType(s"scala.Option[${inner.name}]")

  def apply(datatype: Datatype): ScalaDataType = datatype match {
    case Datatype.StringType => ScalaStringType
    case Datatype.IntegerType => ScalaIntegerType
    case Datatype.DoubleType => ScalaDoubleType
    case Datatype.LongType => ScalaLongType
    case Datatype.BooleanType => ScalaBooleanType
    case Datatype.DecimalType => ScalaDecimalType
    case Datatype.UnitType => ScalaUnitType
    case Datatype.UuidType => ScalaUuidType
    case Datatype.DateTimeIso8601Type => ScalaDateTimeIso8601Type
    case Datatype.MoneyIso4217Type => ScalaMoneyIso4217Type
  }

  def asString(d: ScalaDataType): String = d match {
    case x @ ScalaStringType => "x"
    case x @ ScalaIntegerType => "x.toString"
    case x @ ScalaDoubleType => "x.toString"
    case x @ ScalaLongType => "x.toString"
    case x @ ScalaBooleanType => "x.toString"
    case x @ ScalaDecimalType => "x.toString"
    case x @ ScalaUuidType => "x.toString"
    case x @ ScalaDateTimeIso8601Type => {
      "org.joda.time.format.ISODateTimeFormat.dateTime.print(x)"
    }
    case x @ ScalaMoneyIso4217Type => ???
    case x => throw new UnsupportedOperationException(s"unsupported conversion of type ${d.name} to query string")
  }
}
