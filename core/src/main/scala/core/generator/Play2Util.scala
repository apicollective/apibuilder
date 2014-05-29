package core.generator

import core._
import Text._

trait Play2Util {
  def jsonReads(x: Any): String
  def jsonWrites(x: Any): String
  def queryParams(operation: ScalaOperation): String
  def pathParams(operation: ScalaOperation): String
  def formParams(operation: ScalaOperation): String
}

object Play2Util extends Play2Util {
  import ScalaDataType._

  def jsonReads(x: Any): String = {
    val (name, impl) = x match {
      case m: ScalaModel => m.name -> JsonReadsHelper.readFun(m)
      case d: ScalaDataType => d.name -> JsonReadsHelper.readFun(d)
    }
s"""new play.api.libs.json.Reads[$name] {
  val impl =
${impl.indent(4)}

  def reads(json: play.api.libs.json.JsValue) = {
    try {
      play.api.libs.json.JsSuccess(impl(json))
    } catch {
      case e: Exception => play.api.libs.json.JsError(e.getMessage)
    }
  }
}"""
  }

  def jsonWrites(x: Any): String = {
    val (name, impl) = x match {
      case m: ScalaModel => m.name -> JsonWritesHelper.writeFun(m)
      case d: ScalaDataType => d.name -> JsonWritesHelper.writeFun(d)
    }
s"""new play.api.libs.json.Writes[$name] {
  val impl =
${impl.indent(4)}

  def writes(value: $name) = impl(value)
}"""
  }

  def queryParams(op: ScalaOperation): String = {
    val queryParams = op.parameters.filter(_.location == ParameterLocation.Query)
    val queryStringEntries: String = queryParams.map { p =>
      s"queryBuilder ++= ${QueryStringHelper.queryString(p)}"
    }.mkString("\n")
    s"""val queryBuilder = List.newBuilder[(String, String)]
${queryStringEntries}"""
  }

  def pathParams(op: ScalaOperation): String = {
    val pairs = op.parameters
      .filter(_.location == ParameterLocation.Path)
      .map { p =>
        require(!p.multiple, "Path parameters cannot be lists.")
        require(!p.isOption, "Path parameters cannot be optional.")
        p.originalName -> s"(${QueryStringHelper.urlEncode(p.datatype)})(${p.name})"
      }
    val tmp: String = pairs.foldLeft(op.path) {
      case (path, (name, value)) =>
        val spec = s"/:$name"
        val from = path.indexOfSlice(spec)
        path.patch(from, s"/$${$value}", spec.length)
    }
    s""" s"$tmp" """.trim
  }

  def formParams(op: ScalaOperation): String = {
    val params = op.parameters
      .filter(_.location == ParameterLocation.Form)
      .map(JsonWritesHelper.writeFun).mkString(",\n\n")
    s"""val payload = play.api.libs.json.Json.obj(
${params.indent}
)"""
  }

  private object JsonReadsHelper {
    def readFun(body: String): String = {
  s"""{ json: play.api.libs.json.JsValue =>
${body.indent}
}"""
    }

    def readFun(m: ScalaModel): String = {
      val fields: String = m.fields.map(readFun).mkString(",\n\n")
      readFun(s"""new ${m.name}(
${fields.indent}
)""")
    }

    def readFun(field: ScalaField): String = {
      val dataTypeImpl = readFun(field.datatype)
      val impl = if (field.multiple) {
        readFun(s"""json match {
  case _ @ (play.api.libs.json.JsNull | _ :play.api.libs.json.JsUndefined) =>
    Nil
  case play.api.libs.json.JsArray(values) => values.toList.map { x =>
    (
${dataTypeImpl.indent(6)}
    )(x)
  }
  case x => sys.error(s"cannot parse list from $${x.getClass}")
}""")
      } else if (field.isOption) {
        readFun(s"""json match {
  case _ @ (play.api.libs.json.JsNull | _ :play.api.libs.json.JsUndefined) =>
    None
  case x => Some {
    (
${dataTypeImpl.indent(6)}
    )(x)
  }
}""")
      } else {
        dataTypeImpl
      }
  s"""${field.name} = ($impl)(json \\ "${field.originalName}")"""
    }

    def readFun(d: ScalaDataType): String = d match {
      case x @ ScalaStringType => readFun(s"json.as[${x.name}]")
      case x @ ScalaIntegerType => readFun(s"json.as[${x.name}]")
      case x @ ScalaLongType => readFun(s"json.as[${x.name}]")
      case x @ ScalaBooleanType => readFun(s"json.as[${x.name}]")
      case x @ ScalaDecimalType => readFun(s"json.as[${x.name}]")
      case x @ ScalaUnitType => throw new UnsupportedOperationException("unsupported attempt to read Unit from json")
      case x @ ScalaUuidType => {
        readFun("java.util.UUID.fromString(json.as[String])")
      }
      case x @ ScalaDateTimeIso8601Type => {
        readFun("org.joda.time.format.ISODateTimeFormat.dateTimeParser.parseDateTime(json.as[String])")
      }
      case x @ ScalaMoneyIso4217Type => ???
      case x => readFun(s"json.as[${x.name}]")
    }
  }

  private object JsonWritesHelper {
    def writeFun(body: String)(implicit  d: ScalaDataType): String = {
      s"""{ value: ${d.name} =>
${body.indent}
}"""
    }

    def writeFun(m: ScalaModel): String = {
      implicit val d = new ScalaDataType(m.name)
      val fields: String = m.fields.map(writeFun).mkString(",\n\n")
  writeFun(s"""play.api.libs.json.Json.obj(
${fields.indent}
)""")
    }

    def writeFun(param: ScalaParameter): String = {
      val dataTypeImpl = writeFun(param.datatype)
      val impl = if (param.multiple || param.isOption) {
        s"""${param.name}.map { value =>
  (
${dataTypeImpl.indent(4)}
  )(value)
}"""
      } else {
        s"""($dataTypeImpl)(${param.name})"""
      }
      s""" "${param.originalName}" -> $impl""".trim
    }

    def writeFun(field: ScalaField): String = {
      val dataTypeImpl = writeFun(field.datatype)
      val impl = if (field.multiple || field.isOption) {
        s"""value.${field.name}.map { value =>
  ($dataTypeImpl)(value)
}"""
      } else {
        s"""($dataTypeImpl)(value.${field.name})"""
      }
      s""" "${field.originalName}" -> $impl""".trim
    }

    def writeFun(implicit d: ScalaDataType): String = d match {
      case x @ ScalaStringType => "play.api.libs.json.JsString.apply"
      case x @ ScalaIntegerType => "play.api.libs.json.JsNumber(_)"
      case x @ ScalaLongType => "play.api.libs.json.JsNumber(_)"
      case x @ ScalaBooleanType => "play.api.libs.json.JsBoolean(_)"
      case x @ ScalaDecimalType => "play.api.libs.json.JsNumber.apply"
      case x @ ScalaUnitType => throw new UnsupportedOperationException("unsupported attempt to write Unit to json")
      case x @ ScalaUuidType => {
        writeFun("play.api.libs.json.JsString(value.toString)")
      }
      case x @ ScalaDateTimeIso8601Type => {
        writeFun("""
val ts = org.joda.time.format.ISODateTimeFormat.dateTime.print(value)
play.api.libs.json.JsString(ts)
""")
      }
      case x @ ScalaMoneyIso4217Type => ???
      case x => writeFun(s"play.api.libs.json.Json.toJson(value)")
    }
  }


  private object QueryStringHelper {
    def queryString(p: ScalaParameter): String = {
      (if (p.isOption || p.multiple) {
        p.name
      } else {
        s"Seq(${p.name})"
      }) + s""".map { x =>
  "${p.originalName}" -> (
${urlEncode(p.datatype).indent(4)}
  )(x)
}"""
    }

    def urlEncode(d: ScalaDataType): String = {
      val toString = d match {
        case x @ ScalaStringType => "x"
        case x @ ScalaIntegerType => "x.toString"
        case x @ ScalaLongType => "x.toString"
        case x @ ScalaBooleanType => "x.toString"
        case x @ ScalaDecimalType => "x.toString"
        case x @ ScalaUuidType => "x.toString"
        case x @ ScalaDateTimeIso8601Type => {
          "org.joda.time.format.ISODateTimeFormat.dateTime.print(x)"
        }
        case x @ ScalaMoneyIso4217Type => ???
        case x => throw new UnsupportedOperationException("unsupported conversion of type ${d.name} to query string")
      }
      s"""{ x: ${d.name} =>
  java.net.URLEncoder.encode($toString, "UTF-8")
}"""
    }
  }
}
