package core.generator

import com.gilt.apidocgenerator.models._
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
                             "with", "yield").toSet

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

  def toClassName(
    name: String,
    multiple: Boolean = false
  ) = {
    val baseName = Text.safeName(
      if (name == name.toUpperCase) {
        Text.initCap(Text.splitIntoWords(name).map(_.toLowerCase)).mkString("")
      } else {
        Text.initCap(snakeToCamelCase(name))
      }
    )

    ScalaUtil.quoteNameIfKeyword(
      if (multiple) {
        Text.pluralize(baseName)
      } else {
        baseName
      }
    )

  }

  def toVariable(
    name: String,
    multiple: Boolean = false
  ): String = {
    Text.initLowerCase(toClassName(name, multiple))
  }

  def toDefaultClassName(
    multiple: Boolean = false
  ): String = toClassName("value", multiple = multiple)

  def toDefaultVariable(
    multiple: Boolean = false
  ): String = toVariable("value", multiple = multiple)

}

class ScalaServiceDescription(val serviceDescription: ServiceDescription) {

  val name = ScalaUtil.toClassName(serviceDescription.name)

  val packageName: String = serviceDescription.packageName match {
    case None => ScalaUtil.packageName(serviceDescription.name)
    case Some(name) => name + "." + ScalaUtil.packageName(serviceDescription.name)
  }

  val modelPackageName = s"$packageName.models"
  val enumPackageName = modelPackageName

  def modelClassName(name: String) = modelPackageName + "." + ScalaUtil.toClassName(name)
  def enumClassName(name: String) = enumPackageName + "." + ScalaUtil.toClassName(name)

  val models = serviceDescription.models.map { new ScalaModel(this, _) }

  val enums = serviceDescription.enums.map { new ScalaEnum(_) }

  val packageNamePrivate = packageName.split("\\.").last

  val defaultHeaders: Seq[ScalaHeader] = {
    serviceDescription.headers.filter(!_.default.isEmpty).map { h =>
      ScalaHeader(h.name, h.default.get)
    }
  }

  val resources = serviceDescription.resources.map { new ScalaResource(this, _) }

  def scalaDataType(
    typeInstance: TypeInstance
  ): ScalaDataType = typeInstance match {
    case TypeInstance(Container.Singleton, Type(TypeKind.Primitive, pt)) => ScalaDataType.primitive(pt)
    case TypeInstance(Container.List, Type(TypeKind.Primitive, pt)) => ScalaDataType.ScalaListType(ScalaDataType.primitive(pt))
    case TypeInstance(Container.Map, Type(TypeKind.Primitive, pt)) => ScalaDataType.ScalaMapType(ScalaDataType.primitive(pt))

    case TypeInstance(Container.Singleton, Type(TypeKind.Model, name)) => ScalaDataType.ScalaModelType(modelPackageName, name)
    case TypeInstance(Container.List, Type(TypeKind.Model, name)) => ScalaDataType.ScalaListType(ScalaDataType.ScalaModelType(modelPackageName, name))
    case TypeInstance(Container.Map, Type(TypeKind.Model, name)) => ScalaDataType.ScalaMapType(ScalaDataType.ScalaModelType(modelPackageName, name))

    case TypeInstance(Container.Singleton, Type(TypeKind.Enum, name)) => ScalaDataType.ScalaEnumType(enumPackageName, name)
    case TypeInstance(Container.List, Type(TypeKind.Enum, name)) => ScalaDataType.ScalaListType(ScalaDataType.ScalaEnumType(enumPackageName, name))
    case TypeInstance(Container.Map, Type(TypeKind.Enum, name)) => ScalaDataType.ScalaMapType(ScalaDataType.ScalaEnumType(enumPackageName, name))
  }

}

case class ScalaHeader(name: String, value: String) {
  val quotedValue = s""""$value""""
}


class ScalaModel(val ssd: ScalaServiceDescription, val model: Model) {

  val name: String = ScalaUtil.toClassName(model.name)

  val plural: String = underscoreAndDashToInitCap(model.plural)

  val description: Option[String] = model.description

  val fields = model.fields.map { f => new ScalaField(ssd, this.name, f) }.toList

  val argList: Option[String] = ScalaUtil.fieldsToArgList(fields.map(_.definition))

}

class ScalaBody(val body: TypeInstance) {

  val name: String = body match {
    case TypeInstance(Container.Singleton, Type(TypeKind.Primitive, name)) => ScalaUtil.toDefaultClassName()
    case TypeInstance(Container.List | Container.Map, Type(TypeKind.Primitive, name)) => ScalaUtil.toDefaultClassName(true)

    case TypeInstance(Container.Singleton, Type(TypeKind.Model, name)) => ScalaUtil.toClassName(name)
    case TypeInstance(Container.List | Container.Map, Type(TypeKind.Model, name)) => ScalaUtil.toClassName(name, true)

    case TypeInstance(Container.Singleton, Type(TypeKind.Enum, name)) => ScalaUtil.toClassName(name)
    case TypeInstance(Container.List | Container.Map, Type(TypeKind.Enum, name)) => ScalaUtil.toClassName(name, true)
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

class ScalaResource(ssd: ScalaServiceDescription, resource: Resource) {
  val model = new ScalaModel(ssd, resource.model)

  val packageName: String = ssd.packageName

  val path = resource.path

  val operations = resource.operations.map { op =>
    new ScalaOperation(ssd, model, op, this)
  }
}

class ScalaOperation(val ssd: ScalaServiceDescription, model: ScalaModel, operation: Operation, resource: ScalaResource) {

  val method: String = operation.method

  val path: String = operation.path

  val description: Option[String] = operation.description

  val body: Option[ScalaBody] = operation.body.map(new ScalaBody(_))

  val parameters: List[ScalaParameter] = {
    operation.parameters.toList.map { new ScalaParameter(ssd, _) }
  }

  lazy val pathParameters = parameters.filter { _.location == ParameterLocation.Path }

  lazy val queryParameters = parameters.filter { _.location == ParameterLocation.Query }

  lazy val formParameters = parameters.filter { _.location == ParameterLocation.Form }

  val name: String = GeneratorUtil.urlToMethodName(resource.model.plural, resource.path, operation.method, operation.path)

  val argList: Option[String] = operation.body match {
    case None => ScalaUtil.fieldsToArgList(parameters.map(_.definition))
    case Some(typeInstance) => {
      val sdt = ssd.scalaDataType(typeInstance)
      val varName = typeInstance match {
        case TypeInstance(Container.Singleton, Type(TypeKind.Primitive, pt)) => ScalaUtil.toDefaultVariable()
        case TypeInstance(Container.Singleton, Type(TypeKind.Model, name)) => ScalaUtil.toVariable(name)
        case TypeInstance(Container.Singleton, Type(TypeKind.Enum, name)) => ScalaUtil.toVariable(name)

        case TypeInstance(Container.List | Container.Map, Type(TypeKind.Primitive, name)) => ScalaUtil.toDefaultVariable(true)
        case TypeInstance(Container.List | Container.Map, Type(TypeKind.Model, name)) => ScalaUtil.toVariable(name, true)
        case TypeInstance(Container.List | Container.Map, Type(TypeKind.Enum, name)) => ScalaUtil.toVariable(name, true)
      }

      Some(
        Seq(
          Some(s"%s: %s".format(varName, sdt.name)),
          ScalaUtil.fieldsToArgList(parameters.map(_.definition))
        ).flatten.mkString(", ")
      )
    }
  }

  private def bodyClassArg(
    name: String,
    multiple: Boolean
  ): String = {
    val baseClassName = ssd.modelClassName(name)
    val className = if (multiple) {
      s"scala.collection.Seq[$baseClassName]"
    } else {
      baseClassName
    }

    Seq(
      Some(s"${ScalaUtil.toVariable(name, multiple)}: $className"),
      ScalaUtil.fieldsToArgList(parameters.map(_.definition))
    ).flatten.mkString(", ")
  }

  val responses: Seq[ScalaResponse] = {
    operation.responses.toList.map { new ScalaResponse(ssd, method, _) } 
  }

  lazy val resultType = responses.find(_.isSuccess).map(_.resultType).getOrElse("Unit")

}

class ScalaResponse(ssd: ScalaServiceDescription, method: String, response: Response) {

  val isSingleton = response.`type`.container match {
    case Container.Singleton => true
    case Container.List | Container.Map => false
    case Container.UNDEFINED(_) => false
  }
  val isOption = isSingleton && !Util.isJsonDocumentMethod(method)

  val code = response.code
  val isSuccess = code >= 200 && code < 300
  val isNotFound = code == 404

  val datatype = ssd.scalaDataType(response.`type`)

  val isUnit = datatype == ScalaDataType.ScalaUnitType

  val resultType: String = datatype.name

  val errorVariableName = response.`type` match {
    case TypeInstance(Container.Singleton, Type(TypeKind.Primitive, name)) => ScalaUtil.toDefaultVariable(multiple = false)
    case TypeInstance(Container.List | Container.Map, Type(TypeKind.Primitive, name)) => ScalaUtil.toDefaultVariable(multiple = false)

    case TypeInstance(Container.Singleton, Type(TypeKind.Model, name)) => ScalaUtil.toVariable(name, multiple = false)
    case TypeInstance(Container.List | Container.Map, Type(TypeKind.Model, name)) => ScalaUtil.toVariable(name, multiple = true)

    case TypeInstance(Container.Singleton, Type(TypeKind.Enum, name)) => ScalaUtil.toVariable(name, multiple = false)
    case TypeInstance(Container.List | Container.Map, Type(TypeKind.Enum, name)) => ScalaUtil.toVariable(name, multiple = true)
  }

  val errorClassName = Text.initCap(errorVariableName) + "Response"

}

class ScalaField(ssd: ScalaServiceDescription, modelName: String, field: Field) {

  def name: String = ScalaUtil.quoteNameIfKeyword(snakeToCamelCase(field.name))

  def originalName: String = field.name

  val `type` = field.`type`

  def datatype = ssd.scalaDataType(field.`type`)

  def description: Option[String] = field.description

  /**
   * If there is a default, ensure it is only set server side otherwise
   * changing the default would have no impact on deployed clients
   */
  def isOption: Boolean = !field.required || field.default.nonEmpty

  def definition: String = datatype.definition(field.`type`, name, isOption)
}

class ScalaParameter(ssd: ScalaServiceDescription, param: Parameter) {

  def name: String = ScalaUtil.toVariable(param.name)

  def `type`: TypeInstance = param.`type`

  def originalName: String = param.name

  def datatype = ssd.scalaDataType(param.`type`)
  def description: String = param.description.getOrElse(name)

  val isSingleton = param.`type`.container match {
    case Container.Singleton => true
    case Container.List | Container.Map => false
    case Container.UNDEFINED(_) => false
  }

  /**
   * If there is a default, ensure it is only set server side otherwise
   * changing the default would have no impact on deployed clients
   */
  def isOption: Boolean = !param.required || param.default.nonEmpty

  def definition: String = datatype.definition(param.`type`, name, isOption)

  def location = param.location
}

sealed abstract class ScalaDataType(val name: String) {

  def definition(
    typeInstance: TypeInstance,
    varName: String,
    optional: Boolean
  ): String = {
    if (optional) {
      s"$varName: scala.Option[$name]" + " = " + nilValue(typeInstance)
    } else {
      s"$varName: $name"
    }
  }

  def nilValue(typeInstance: TypeInstance): String = typeInstance.container match {
    case Container.Singleton => "None"
    case Container.List => "Nil"
    case Container.Map => "Map.Empty"
    case Container.UNDEFINED(container) => sys.error(s"Invalid container[$container]")
  }

}

object ScalaDataType {

  case object ScalaStringType extends ScalaDataType("String")
  case object ScalaIntegerType extends ScalaDataType("Int")
  case object ScalaDoubleType extends ScalaDataType("Double")
  case object ScalaLongType extends ScalaDataType("Long")
  case object ScalaBooleanType extends ScalaDataType("Boolean")
  case object ScalaDecimalType extends ScalaDataType("BigDecimal")
  case object ScalaUnitType extends ScalaDataType("Unit")

  case object ScalaUuidType extends ScalaDataType("_root_.java.util.UUID")
  case object ScalaDateIso8601Type extends ScalaDataType("_root_.org.joda.time.LocalDate")
  case object ScalaDateTimeIso8601Type extends ScalaDataType("_root_.org.joda.time.DateTime")

  case class ScalaModelType(packageName: String, modelName: String) extends ScalaDataType(s"${packageName}.${ScalaUtil.toClassName(modelName)}")
  case class ScalaEnumType(packageName: String, enumName: String) extends ScalaDataType(s"${packageName}.${ScalaUtil.toClassName(enumName)}")
  case class ScalaListType(inner: ScalaDataType) extends ScalaDataType(s"scala.collection.Seq[${inner.name}]")
  case class ScalaMapType(inner: ScalaDataType) extends ScalaDataType(s"scala.collection.Map[String, ${inner.name}]")

  def primitive(name: String): ScalaDataType = {
    Primitives(name).getOrElse {
      sys.error(s"There is no primitive named[$name]")
    } match {
      case Primitives.String => ScalaStringType
      case Primitives.DateIso8601 => ScalaDateIso8601Type
      case Primitives.DateTimeIso8601 => ScalaDateTimeIso8601Type
      case Primitives.Uuid => ScalaUuidType
      case Primitives.Integer => ScalaIntegerType
      case Primitives.Double => ScalaDoubleType
      case Primitives.Long => ScalaLongType
      case Primitives.Boolean => ScalaBooleanType
      case Primitives.Decimal => ScalaDecimalType
      case Primitives.Unit => ScalaUnitType
    }
  }

  def asString(varName: String, d: ScalaDataType): String = d match {
    case ScalaStringType => s"$varName"
    case ScalaIntegerType => s"$varName.toString"
    case ScalaDoubleType => s"$varName.toString"
    case ScalaLongType => s"$varName.toString"
    case ScalaBooleanType => s"$varName.toString"
    case ScalaDecimalType => s"$varName.toString"
    case ScalaUuidType => s"$varName.toString"
    case ScalaDateIso8601Type => s"$varName.toString"
    case ScalaDateTimeIso8601Type => {
      s"org.joda.time.format.ISODateTimeFormat.dateTime.print($varName)"
    }
    case ScalaEnumType(_, _) => s"$varName.toString"
    case _ => throw new UnsupportedOperationException(s"unsupported conversion of type ${d} to query string for $varName")
  }

}
