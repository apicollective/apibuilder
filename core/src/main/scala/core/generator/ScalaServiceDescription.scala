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

  def fieldsToArgList(fields: Seq[String]): Option[String] = {
    if (fields.isEmpty) {
      None
    } else {
      Some(fields.map(_.indent).mkString("\n", ",\n", "\n"))
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

  def toClassName(name: String) = {
    ScalaUtil.quoteNameIfKeyword(
      Text.safeName(
        if (name == name.toUpperCase) {
          Text.initCap(name.split("_").map(_.toLowerCase)).mkString("")
        } else {
          Text.initCap(snakeToCamelCase(name))
        }
      )
    )
  }

}

class ScalaServiceDescription(val serviceDescription: ServiceDescription, metadata: Option[OrganizationMetadata] = None) {

  val name = safeName(serviceDescription.name)

  val models = serviceDescription.models.map { new ScalaModel(serviceDescription, _) }

  val enums = serviceDescription.enums.map { new ScalaEnum(_) }

  val packageName: String = metadata.flatMap(_.package_name) match {
    case None => ScalaUtil.packageName(serviceDescription.name)
    case Some(name) => name + "." + ScalaUtil.packageName(serviceDescription.name)
  }

  val resources = serviceDescription.resources.map { new ScalaResource(serviceDescription, packageName, _) }

}

class ScalaModel(val serviceDescription: ServiceDescription, val model: Model) {

  val name: String = ScalaUtil.toClassName(model.name)

  val plural: String = underscoreAndDashToInitCap(model.plural)

  val description: Option[String] = model.description

  val fields = model.fields.map { f => new ScalaField(this.name, f) }.toList

  val argList: Option[String] = ScalaUtil.fieldsToArgList(fields.map(_.definition))

}

class ScalaBody(val body: Body) {

  val name: String = body match {
    case PrimitiveBody(dt) => "value"
    case ModelBody(name) => ScalaUtil.toClassName(name)
    case EnumBody(name) => ScalaUtil.toClassName(name)
  }

}

class ScalaEnum(val enum: Enum) {

  val name: String = ScalaUtil.toClassName(enum.name)

  val description: Option[String] = enum.description

  val values: Seq[ScalaEnumValue] = enum.values.map { new ScalaEnumValue(_) }

}

class ScalaEnumValue(value: EnumValue) {

  val originalName: String = value.name

  val name: String = ScalaUtil.toClassName(value.name)

  val description: Option[String] = value.description

}

class ScalaResource(serviceDescription: ServiceDescription, val packageName: String, resource: Resource) {
  val model = new ScalaModel(serviceDescription, resource.model)

  val path = resource.path

  val operations = resource.operations.map { op =>
    new ScalaOperation(serviceDescription, model, op, this)
  }
}

class ScalaOperation(serviceDescription: ServiceDescription, model: ScalaModel, operation: Operation, resource: ScalaResource) {

  val method: String = operation.method

  val path: String = operation.path

  val description: Option[String] = operation.description

  val body: Option[ScalaBody] = operation.body.map(new ScalaBody(_))

  val parameters: List[ScalaParameter] = {
    operation.parameters.toList.map { new ScalaParameter(_) }
  }

  lazy val pathParameters = parameters.filter { _.location == ParameterLocation.Path }

  lazy val queryParameters = parameters.filter { _.location == ParameterLocation.Query }

  lazy val formParameters = parameters.filter { _.location == ParameterLocation.Form }

  val name: String = GeneratorUtil.urlToMethodName(resource.path, operation.method, operation.path)

  val argList: Option[String] = operation.body match {
    case None => ScalaUtil.fieldsToArgList(parameters.map(_.definition))
    case Some(PrimitiveBody(dt)) => {
      val typeName = ScalaDataType(dt).name
      Some(
        Seq(
          Some(s"value: $typeName"),
          ScalaUtil.fieldsToArgList(parameters.map(_.definition))
        ).flatten.mkString(", ")
      )
    }
    case Some(ModelBody(name)) => Some(bodyClassArg(name))
    case Some(EnumBody(name)) => Some(bodyClassArg(name))
  }

  private def bodyClassArg(name: String): String = {
    Seq(
      Some(s"${Text.initLowerCase(name)}: ${resource.packageName}.models.${name}"),
      ScalaUtil.fieldsToArgList(parameters.map(_.definition))
    ).flatten.mkString(", ")
  }

  val responses: Seq[ScalaResponse] = {
    operation.responses.toList.map { new ScalaResponse(resource.packageName, method, _) } 
  }

  lazy val resultType = responses.find(_.isSuccess).map(_.resultType).getOrElse("Unit")

}

class ScalaResponse(packageName: String, method: String, response: Response) {

  val scalaType: String = underscoreAndDashToInitCap(response.datatype)
  val isUnit = scalaType == "Unit"
  val isMultiple = response.multiple
  val isOption = !isMultiple && !Util.isJsonDocumentMethod(method)

  val code = response.code
  val isSuccess = code >= 200 && code < 300
  val isNotFound = code == 404

  val qualifiedScalaType: String = if (isUnit) { scalaType } else { packageName + ".models." + scalaType }

  val resultType: String = {
    if (response.multiple) {
      s"scala.collection.Seq[${qualifiedScalaType}]"
    } else if (isOption) {
      s"scala.Option[$qualifiedScalaType]"
    } else {
      qualifiedScalaType
    }
  }

  private val underscore = Text.camelCaseToUnderscore(scalaType)
  val errorVariableName = if (isMultiple) {
    Text.snakeToCamelCase(Text.pluralize(underscore.toLowerCase))
  } else {
    Text.snakeToCamelCase(underscore.toLowerCase)
  }

  val errorClassName = Text.initCap(errorVariableName) + "Response"

}

class ScalaField(modelName: String, field: Field) {

  def name: String = ScalaUtil.quoteNameIfKeyword(snakeToCamelCase(field.name))

  def originalName: String = field.name

  import ScalaDataType._
  val baseType: ScalaDataType = field.fieldtype match {
    case t: PrimitiveFieldType => ScalaDataType(t.datatype)
    case m: ModelFieldType => new ScalaModelType(m.modelName)
    case e: EnumFieldType => new ScalaEnumType(e.enum.name)
  }

  def datatype: ScalaDataType = {
    if (multiple) {
      new ScalaListType(baseType)
    } else if (isOption) {
      new ScalaOptionType(baseType)
    } else {
      baseType
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
      case m: ModelParameterType => new ScalaModelType(m.model.name)
      case e: EnumParameterType => new ScalaEnumType(e.enum.name)
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
  case class ScalaModelType(modelName: String) extends ScalaDataType(ScalaUtil.toClassName(modelName))
  case class ScalaEnumType(enumName: String) extends ScalaDataType(ScalaUtil.toClassName(enumName))
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
    case ScalaStringType => s"$varName"
    case ScalaIntegerType => s"$varName.toString"
    case ScalaDoubleType => s"$varName.toString"
    case ScalaLongType => s"$varName.toString"
    case ScalaBooleanType => s"$varName.toString"
    case ScalaDecimalType => s"$varName.toString"
    case ScalaUuidType => s"$varName.toString"
    case ScalaDateTimeIso8601Type => {
      s"org.joda.time.format.ISODateTimeFormat.dateTime.print($varName)"
    }
    case ScalaEnumType(_) => s"$varName.toString"
    case _ => throw new UnsupportedOperationException(s"unsupported conversion of type ${d} to query string for $varName")
  }
}
