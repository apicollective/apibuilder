package core.generator

import core._
import Text._

trait Play2Util {
  def jsonReads(x: ScalaModel): String
  def jsonWrites(x: ScalaModel): String
  def queryParams(operation: ScalaOperation): String
  def pathParams(operation: ScalaOperation): String
}

object Play2Util extends Play2Util {
  import ScalaDataType._

  def jsonReads(x: ScalaModel): String = {
    val name = x.name
    val classReads: String = {
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
          s"""implicit val reads$name = {
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

          s"""implicit val reads$name = {
  import play.api.libs.json._
  import play.api.libs.functional.syntax._
${builder.indent}(${name}.apply _)
}"""
        }
      }
    }
    val patchReads: String = {
      def read(field: ScalaField): String = {
        s"readNullable[${field.datatype.name}]"
      }
      x.fields match {
        case field::Nil => {
          s"""implicit val reads${name}_Patch = {
  import play.api.libs.json._
  import play.api.libs.functional.syntax._
  (__ \\ "${field.originalName}").${read(field)}.map { x =>
    new ${name}.Patch(${field.name} = x)
  }
}"""
        }
        case fields => {
          val builder: String = x.fields.map { field =>
            s"""(__ \\ "${field.originalName}").${read(field)}"""
          }.mkString("(", " and\n ", ")")

          s"""implicit val reads${name}_Patch = {
  import play.api.libs.json._
  import play.api.libs.functional.syntax._
${builder.indent}(${name}.Patch.apply _)
}"""
        }
      }
    }

    s"""$classReads

$patchReads
"""
  }

  def jsonWrites(x: ScalaModel): String = {
    val name = x.name
    val classWrites: String = {
      x.fields match {
        case field::Nil => {
          s"""implicit val writes$name = new play.api.libs.json.Writes[$name] {
  def writes(x: ${name}) = play.api.libs.json.Json.obj(
    "${field.originalName}" -> play.api.libs.json.Json.toJson(x.${field.name})
  )
}"""
        }
        case fields => {
          val builder: String = x.fields.map { field =>
            s"""(__ \\ "${field.originalName}").write[${field.datatype.name}]"""
          }.mkString("(", " and\n ", ")")

          s"""implicit val writes$name = {
  import play.api.libs.json._
  import play.api.libs.functional.syntax._
${builder.indent}(unlift(${name}.unapply))
}"""
        }
      }
    }

    val patchWrites = {
      x.fields match {
        case field::Nil => {
          s"""implicit val writes${name}_Patch = new play.api.libs.json.Writes[$name.Patch] {
  def writes(x: ${name}.Patch) = play.api.libs.json.Json.obj(
    "${field.originalName}" -> play.api.libs.json.Json.toJson(x.${field.name})
  )
}"""
        }
        case fields => {
          val builder: String = x.fields.map { field =>
            s"""(__ \\ "${field.originalName}").writeNullable[${field.datatype.name}]"""
          }.mkString("(", " and\n ", ")")

          s"""implicit val writes${name}_Patch = {
  import play.api.libs.json._
  import play.api.libs.functional.syntax._
${builder.indent}(unlift(${name}.Patch.unapply))
}"""
        }
      }
    }
    s"""$classWrites

$patchWrites
"""
  }

  // TODO refactor so this doesn't use a List Builder and anonymous functions
  def queryParams(op: ScalaOperation): String = {
    val queryStringEntries: String = op.queryParameters.map { p =>
      s"queryBuilder ++= ${QueryStringHelper.queryString(p)}"
    }.mkString("\n")
    s"""val queryBuilder = List.newBuilder[(String, String)]
${queryStringEntries}"""
  }

  def pathParams(op: ScalaOperation): String = {
    val pairs = op.pathParameters
      .map { p =>
        require(!p.multiple, "Path parameters cannot be lists.")
        p.originalName -> s"(${PathParamHelper.urlEncode(p.datatype)})(${p.name})"
      }
    val tmp: String = pairs.foldLeft(op.path) {
      case (path, (name, value)) =>
        val spec = s"/:$name"
        val from = path.indexOfSlice(spec)
        path.patch(from, s"/$${$value}", spec.length)
    }
    s""" s"$tmp" """.trim
  }

  private object PathParamHelper {
    def urlEncode(d: ScalaDataType): String = {
      s"""{x: ${d.name} =>
  val s = ${ScalaDataType.asString(d)}
  java.net.URLEncoder.encode(s, "UTF-8")
}"""
    }
  }


  private object QueryStringHelper {
    def queryString(p: ScalaParameter): String = {
      val (lhs, dt) = p.datatype match {
        case x: ScalaListType => p.name -> x.inner
        case x: ScalaOptionType => p.name -> x.inner
        case x => s"Seq(${p.name})" -> x
      }
      s"""$lhs.map { x =>
  "${p.originalName}" -> (
${queryString(dt).indent(4)}
  )(x)
}"""
    }

    def queryString(d: ScalaDataType): String = {
      s"""{ x: ${d.name} =>
  ${ScalaDataType.asString(d)}
}"""
    }
  }
}
