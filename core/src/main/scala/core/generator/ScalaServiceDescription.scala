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
    if (fields.isEmpty) {
      ""
    } else {
      fields.map(_.indent).mkString("\n", ",\n", "\n")
    }
  }

  def quoteNameIfKeyword(name: String): String = {
    if (Keywords.contains(name)) {
      "`" + name + "`"
    } else {
      name
    }
  }

  def packageName(serviceName: String): String = {
    Text.safeName(serviceName).toLowerCase
  }
}

class ScalaServiceDescription(val serviceDescription: ServiceDescription) {

  val name = safeName(serviceDescription.name)

  val models = serviceDescription.models.map { new ScalaModel(_) }

  val packageName = ScalaUtil.packageName(serviceDescription.name)

  val resources = serviceDescription.resources.map { new ScalaResource(packageName, _) }

}

class ScalaModel(val model: Model) {

  val name: String = underscoreToInitCap(model.name)

  val plural: String = underscoreToInitCap(model.plural)

  val description: Option[String] = model.description

  val fields = model.fields.map { f => new ScalaField(this.name, f) }

  val argList = ScalaUtil.fieldsToArgList(fields.map(_.definition))

}

class ScalaResource(val packageName: String, resource: Resource) {
  val model = new ScalaModel(resource.model)

  val path = resource.path

  val operations = resource.operations.map { op =>
    new ScalaOperation(model, op, this)
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

  lazy val formParameters = parameters.filter { _.location == ParameterLocation.Form }

  val name: String = GeneratorUtil.urlToMethodName(resource.path, operation.method, operation.path)

  val argList: String = ScalaUtil.fieldsToArgList(parameters.map(_.definition))

  val responses: Seq[ScalaResponse] = {
    operation.responses.toList.map { new ScalaResponse(resource.packageName, method, _) } 
  }

  lazy val resultType = responses.find(_.isSuccess).map(_.resultType).getOrElse("Unit")

}

class ScalaResponse(packageName: String, method: String, response: Response) {

  val scalaType: String = underscoreToInitCap(response.datatype)
  val isUnit = scalaType == "Unit"
  val isMultiple = response.multiple
  val isOption = !isMultiple && !GeneratorUtil.isJsonDocumentMethod(method)

  val code = response.code
  val isSuccess = code >= 200 && code < 300
  val isNotFound = code == 404

  val qualifiedScalaType: String = if (isUnit) { scalaType } else { packageName + ".models." + scalaType }

  val resultType: String = {
    if (response.multiple) {
      s"scala.collection.Seq[${qualifiedScalaType}]"
    } else if (isOption) {
      s"Option[$qualifiedScalaType]"
    } else {
      qualifiedScalaType
    }
  }

  private val underscore = Text.camelCaseToUnderscore(scalaType)
  val errorVariableName = if (isMultiple) {
    Text.snakeToCamelCase(Text.pluralize(underscore.toLowerCase))
  } else {
    Text.snakeToCamelCase(underscore)
  }

  val errorClassName = Text.initCap(errorVariableName) + "Response"

}

class ScalaField(modelName: String, field: Field) {

  def name: String = ScalaUtil.quoteNameIfKeyword(snakeToCamelCase(field.name))

  def originalName: String = field.name

  def datatype: ScalaDataType = {
    import ScalaDataType._
    val base: ScalaDataType = field.fieldtype match {
      case t: PrimitiveFieldType => ScalaDataType(t.datatype)
      case m: ModelFieldType => new ScalaModelType(new ScalaModel(m.model))
      case e: EnumerationFieldType => new ScalaEnumerationType("%s.%s".format(modelName, Text.initCap(Text.snakeToCamelCase(field.name))), ScalaDataType(e.datatype))
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

  def baseType: ScalaDataType = {
    import ScalaDataType._
    param.paramtype match {
      case t: PrimitiveParameterType => ScalaDataType(t.datatype)
      case m: ModelParameterType => new ScalaModelType(new ScalaModel(m.model))
    }
  }

  def datatype: ScalaDataType = {
    import ScalaDataType._
    if (multiple) {
      new ScalaListType(baseType)
    } else if (isOption) {
      new ScalaOptionType(baseType)
    } else {
      baseType
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
  case object ScalaMapType extends ScalaDataType("Map[String, String]")
  case object ScalaUnitType extends ScalaDataType("Unit")
  case object ScalaUuidType extends ScalaDataType("java.util.UUID")
  case object ScalaDateTimeIso8601Type extends ScalaDataType("org.joda.time.DateTime")

  case class ScalaListType(inner: ScalaDataType) extends ScalaDataType(s"scala.collection.Seq[${inner.name}]")
  case class ScalaModelType(model: ScalaModel) extends ScalaDataType(model.name)
  case class ScalaEnumerationType(fieldName: String, inner: ScalaDataType) extends ScalaDataType(fieldName)
  case class ScalaOptionType(inner: ScalaDataType) extends ScalaDataType(s"scala.Option[${inner.name}]")

  def apply(datatype: Datatype): ScalaDataType = datatype match {
    case Datatype.StringType => ScalaStringType
    case Datatype.IntegerType => ScalaIntegerType
    case Datatype.DoubleType => ScalaDoubleType
    case Datatype.LongType => ScalaLongType
    case Datatype.BooleanType => ScalaBooleanType
    case Datatype.DecimalType => ScalaDecimalType
    case Datatype.MapType => ScalaMapType
    case Datatype.UnitType => ScalaUnitType
    case Datatype.UuidType => ScalaUuidType
    case Datatype.DateTimeIso8601Type => ScalaDateTimeIso8601Type
  }

  def asString(varName: String, d: ScalaDataType): String = d match {
    case x @ ScalaStringType => s"$varName"
    case x @ ScalaIntegerType => s"$varName.toString"
    case x @ ScalaDoubleType => s"$varName.toString"
    case x @ ScalaLongType => s"$varName.toString"
    case x @ ScalaBooleanType => s"$varName.toString"
    case x @ ScalaDecimalType => s"$varName.toString"
    case x @ ScalaUuidType => s"$varName.toString"
    case x @ ScalaDateTimeIso8601Type => {
      s"org.joda.time.format.ISODateTimeFormat.dateTime.print($varName)"
    }
    case x => throw new UnsupportedOperationException(s"unsupported conversion of type ${d.name} to query string")
  }
}
