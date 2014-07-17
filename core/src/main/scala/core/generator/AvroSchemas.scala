package core.generator

import core._

object AvroSchemas {
  def apply(json: String): String = {
    apply(ServiceDescription(json))
  }

  def apply(sd: ServiceDescription): String =
    apply(new AvroServiceDescription(sd))

  def apply(asd: AvroServiceDescription): String =
    new AvroSchema(asd).schema

}

class AvroSchema(asd: AvroServiceDescription) {
  import collection.mutable.{Set => MSet}

  def schema =
    asd.models.map(genModel(_, MSet.empty[String])).mkString("\n")

  private def genModel(model: AvroModel, knownTypes: MSet[String]): String =
    getOrCalc(s"${model.namespace.getOrElse(asd.namespace)}.${model.name}", knownTypes) {
s"""{
  "type": "record",
  "name": "${model.name}",
  "namespace": "${model.namespace.getOrElse(asd.namespace)}",
  "fields": [
    ${model.fields.map(genField(_, knownTypes)).mkString(",\n    ")}
  ]
}"""
    }

  private def genField(field: AvroField, knownTypes: MSet[String]): String = {
    val typeSpec = genType(field.fieldtype, knownTypes)
    val defaultSpec = field.default.map {d => s""", "default": "$d"""" }.getOrElse("")

    s"""{"name": "${field.name}", "type": ${typeSpec}${defaultSpec}}"""
  }

  private def genType(t: AvroType, knownTypes: MSet[String]): String = t match {
    case p: AvroPrimitiveType => s""""${p.name}""""
    case AvroRecordType(model) => genModel(model, knownTypes)
    case AvroEnumType(symbols) => s""""enum", "symbols": [${symbols.mkString("\"","\", \"","\"")}]"""
    case AvroArrayType(itemType) => s""""array", "items": ${genType(itemType, knownTypes)}"""
    case AvroMapType(valueType) => s""""map", "values": ${genType(valueType, knownTypes)}"""
    case AvroUnionType(alternatives) => alternatives.map(genType(_,knownTypes)).mkString("[",", ","]")
    case AvroFixedType(name, namespace, size) => getOrCalc(s"$namespace.$name", knownTypes) {
      s"""{"namespace": "$namespace", "type": "fixed", "size": $size, "name": "$name"}"""
    }
  }


  private def getOrCalc(typeName: String, knownTypes: MSet[String])(desc: => String) =
    if (knownTypes.contains(typeName)) {
      s""""$typeName""""
    } else {
      knownTypes += typeName
      desc
    }

}
