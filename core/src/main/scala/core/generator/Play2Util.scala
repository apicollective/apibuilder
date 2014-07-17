package core.generator

import core._
import Text._

object Play2Util {
  import ScalaDataType._

  def jsonReads(x: ScalaModel): String = {
    val name = x.name
    def read(field: ScalaField): String = field.datatype match {
      case x: ScalaDataType.ScalaListType => {
        // if the key is absent, we return an empty
        // list
        s"""readNullable[${x.name}].map { x =>
  x.getOrElse(Nil)
}"""
      }
      case ScalaDataType.ScalaOptionType(inner) => {
        s"readNullable[${inner.name}]"
      }
      case x => {
        s"read[${x.name}]"
      }
    }
    x.fields match {
      case field::Nil => {
        s"""{
  import play.api.libs.json._
  import play.api.libs.functional.syntax._
  (__ \\ "${field.originalName}").${read(field)}.map { x =>
    new ${name}(${field.name} = x)
  }
}"""
      }
      case fields => {
        val builder: String = x.fields.map { field =>
          s"""(__ \\ "${field.originalName}").${read(field)}"""
        }.mkString("(", " and\n ", ")")

        s"""{
  import play.api.libs.json._
  import play.api.libs.functional.syntax._
${builder.indent}(${name}.apply _)
}"""
      }
    }
  }

  def jsonWrites(x: ScalaModel): String = {
    val name = x.name

    x.fields match {
      case field::Nil => {
        s"""new play.api.libs.json.Writes[$name] {
  def writes(x: ${name}) = play.api.libs.json.Json.obj(
    "${field.originalName}" -> play.api.libs.json.Json.toJson(x.${field.name})
  )
}"""
      }
      case fields => {
        val builder: String = x.fields.map { field =>
          s"""(__ \\ "${field.originalName}").write[${field.datatype.name}]"""
        }.mkString("(", " and\n ", ")")

        s"""{
  import play.api.libs.json._
  import play.api.libs.functional.syntax._
${builder.indent}(unlift(${name}.unapply))
}"""
      }
    }
  }

  def queryParams(op: ScalaOperation): Option[String] = {
    if (op.queryParameters.isEmpty) {
      None
    } else {
      Some(
        Seq(
          "val query = Seq(",
          op.queryParameters.map { p =>
            if (p.isOption) {
              s"""  ${p.name}.map("${p.originalName}" -> ${ScalaDataType.asString("_", p.baseType)})"""
            } else {
              s"""  "${p.originalName}" -> ${ScalaDataType.asString(p.name, p.baseType)}"""
            }
          }.mkString(",\n"),
          ").flatten"
        ).mkString("\n")
      )
    }
  }
  
  def pathParams(op: ScalaOperation): String = {
    val pairs = op.pathParameters.map { p =>
      require(!p.multiple, "Path parameters cannot be lists.")
      p.originalName -> PathParamHelper.urlEncode(p.name, p.datatype)
    }
    val tmp: String = pairs.foldLeft(op.path) {
      case (path, (name, value)) =>
        val spec = s"/:$name"
        val from = path.indexOfSlice(spec)
        path.patch(from, s"/$${$value}", spec.length)
    }
    s""" s"$tmp" """.trim
  }

  def formBody(op: ScalaOperation): Option[String] = {
    // Can have both or form params but not both as we can only send a single document
    assert(op.body.isEmpty || op.formParameters.isEmpty)

    if (op.formParameters.isEmpty && op.body.isEmpty) {
      None

    } else if (!op.body.isEmpty) {
      Some(s"val payload = play.api.libs.json.Json.toJson(${Text.initLowerCase(op.body.get.name)})")

    } else {
      val params = op.formParameters.map { param =>
        s""" "${param.originalName}" -> play.api.libs.json.Json.toJson(${param.name})""".trim
      }.mkString(",\n")
      Some(
        Seq(
          "val payload = play.api.libs.json.Json.obj(",
          params.indent,
          ")"
        ).mkString("\n")
      )
    }
  }

  private object PathParamHelper {
    def urlEncode(name: String, d: ScalaDataType): String = {
      d match {
        case ScalaStringType => s"""java.net.URLEncoder.encode($name, "UTF-8")"""
        case ScalaIntegerType | ScalaDoubleType | ScalaLongType | ScalaBooleanType | ScalaDecimalType | ScalaUuidType => name
        case t => {
          sys.error(s"Cannot encode params of type[$t] as path parameters (name: $name)")
        }
      }
    }
  }

}
