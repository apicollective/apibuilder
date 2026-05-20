package io.apibuilder.openapi

import io.apibuilder.validation.ScalarType
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sttp.apispec.{ExampleSingleValue, Schema, SchemaType}

import scala.collection.immutable.ListMap

class SchemaConverterSpec extends AnyWordSpec with Matchers {

  "classify" must {

    "Object when schema has properties" in {
      SchemaClassifier.classifySchema(Schema(properties = ListMap("id" -> Schema()))) must be(SchemaKind.Object)
    }

    "Object when schema has type object" in {
      SchemaClassifier.classifySchema(Schema(`type` = Some(List(SchemaType.Object)))) must be(SchemaKind.Object)
    }

    "Object when schema has both type object and properties" in {
      val s = Schema(
        `type` = Some(List(SchemaType.Object)),
        properties = ListMap("name" -> Schema(`type` = Some(List(SchemaType.String)))),
      )
      SchemaClassifier.classifySchema(s) must be(SchemaKind.Object)
    }

    "StringEnum when schema is string with enum values" in {
      val s = Schema(
        `type` = Some(List(SchemaType.String)),
        `enum` = Some(List(ExampleSingleValue("A"), ExampleSingleValue("B"))),
      )
      SchemaClassifier.classifySchema(s) must be(SchemaKind.StringEnum)
    }

    "Array when schema has type array" in {
      SchemaClassifier.classifySchema(Schema(`type` = Some(List(SchemaType.Array)))) must be(SchemaKind.Array)
    }

    "Alias when schema is a pure $ref" in {
      SchemaClassifier.classifySchema(Schema($ref = Some("#/components/schemas/Foo"))) must be(SchemaKind.Alias)
    }

    "Alias when schema is allOf without properties" in {
      val s = Schema(allOf = List(Schema($ref = Some("#/components/schemas/Bar"))))
      SchemaClassifier.classifySchema(s) must be(SchemaKind.Alias)
    }

    "Union when schema is oneOf without properties" in {
      val s = Schema(oneOf = List(Schema($ref = Some("#/components/schemas/Baz"))))
      SchemaClassifier.classifySchema(s) must be(SchemaKind.Union)
    }

    "Alias when schema is plain string without enum" in {
      SchemaClassifier.classifySchema(Schema(`type` = Some(List(SchemaType.String)))) must be(SchemaKind.Alias)
    }

    "Object takes priority over allOf when properties are present" in {
      val s = Schema(
        properties = ListMap("id" -> Schema()),
        allOf = List(Schema($ref = Some("#/components/schemas/Base"))),
      )
      SchemaClassifier.classifySchema(s) must be(SchemaKind.Object)
    }

    "Skip when schema has only examples and no type" in {
      val s = Schema(examples = Some(List(ExampleSingleValue("example"))))
      SchemaClassifier.classifySchema(s) must be(SchemaKind.Skip)
    }

    "Skip for empty schema" in {
      SchemaClassifier.classifySchema(Schema()) must be(SchemaKind.Skip)
    }
  }

  "classifyField" must {

    "Ref for schema with $ref" in {
      SchemaClassifier.classifyField(Schema($ref = Some("#/components/schemas/Address"))) must be(
        Some(FieldKind.Ref("Address")),
      )
    }

    "AllOfRef for schema with allOf containing $ref" in {
      val s = Schema(allOf = List(
        Schema($ref = Some("#/components/schemas/Base")),
        Schema(description = Some("extra")),
      ))
      SchemaClassifier.classifyField(s) must be(Some(FieldKind.AllOfRef("Base")))
    }

    "Number for number type" in {
      SchemaClassifier.classifyField(Schema(`type` = Some(List(SchemaType.Number)))) must be(
        Some(FieldKind.Number),
      )
    }

    "ArrayRef for array with $ref items" in {
      val s = Schema(
        `type` = Some(List(SchemaType.Array)),
        items = Some(Schema($ref = Some("#/components/schemas/LineItem"))),
      )
      SchemaClassifier.classifyField(s) must be(Some(FieldKind.ArrayRef("LineItem")))
    }

    "ArrayEnum for array with inline string enum items" in {
      val items = Schema(
        `type` = Some(List(SchemaType.String)),
        `enum` = Some(List(ExampleSingleValue("A"), ExampleSingleValue("B"))),
      )
      val s = Schema(`type` = Some(List(SchemaType.Array)), items = Some(items))
      SchemaClassifier.classifyField(s) must be(Some(FieldKind.ArrayEnum(items)))
    }

    "InlineEnum for string with enum values" in {
      val s = Schema(
        `type` = Some(List(SchemaType.String)),
        `enum` = Some(List(ExampleSingleValue("X"), ExampleSingleValue("Y"))),
      )
      SchemaClassifier.classifyField(s) must be(Some(FieldKind.InlineEnum(s)))
    }

    "ArraySimple for array with simple string items" in {
      val s = Schema(
        `type` = Some(List(SchemaType.Array)),
        items = Some(Schema(`type` = Some(List(SchemaType.String)))),
      )
      SchemaClassifier.classifyField(s) must be(Some(FieldKind.ArraySimple(ScalarType.StringType)))
    }

    "ArraySimple for array with integer items" in {
      val s = Schema(
        `type` = Some(List(SchemaType.Array)),
        items = Some(Schema(`type` = Some(List(SchemaType.Integer)))),
      )
      SchemaClassifier.classifyField(s) must be(Some(FieldKind.ArraySimple(ScalarType.IntegerType)))
    }

    "Primitive for boolean" in {
      SchemaClassifier.classifyField(Schema(`type` = Some(List(SchemaType.Boolean)))) must be(
        Some(FieldKind.Primitive(ScalarType.BooleanType)),
      )
    }

    "Primitive for string" in {
      SchemaClassifier.classifyField(Schema(`type` = Some(List(SchemaType.String)))) must be(
        Some(FieldKind.Primitive(ScalarType.StringType)),
      )
    }

    "Primitive for integer" in {
      SchemaClassifier.classifyField(Schema(`type` = Some(List(SchemaType.Integer)))) must be(
        Some(FieldKind.Primitive(ScalarType.IntegerType)),
      )
    }

    "None for empty schema" in {
      SchemaClassifier.classifyField(Schema()) must be(None)
    }

    "Ref takes priority over simple type" in {
      val s = Schema(
        $ref = Some("#/components/schemas/Foo"),
        `type` = Some(List(SchemaType.String)),
      )
      SchemaClassifier.classifyField(s) must be(Some(FieldKind.Ref("Foo")))
    }

    "AllOfRef takes priority over number" in {
      val s = Schema(
        allOf = List(Schema($ref = Some("#/components/schemas/Amount"))),
        `type` = Some(List(SchemaType.Number)),
      )
      SchemaClassifier.classifyField(s) must be(Some(FieldKind.AllOfRef("Amount")))
    }

    "Number takes priority over simple" in {
      val s = Schema(`type` = Some(List(SchemaType.Number)))
      SchemaClassifier.classifyField(s) must be(Some(FieldKind.Number))
      SchemaConverter.simpleType(s) must be(Some(ScalarType.DecimalType))
    }

    "ArrayRef takes priority over ArraySimple" in {
      val s = Schema(
        `type` = Some(List(SchemaType.Array)),
        items = Some(Schema($ref = Some("#/components/schemas/Thing"))),
      )
      SchemaClassifier.classifyField(s) must be(Some(FieldKind.ArrayRef("Thing")))
    }

    "InlineEnum takes priority over Simple string" in {
      val s = Schema(
        `type` = Some(List(SchemaType.String)),
        `enum` = Some(List(ExampleSingleValue("on"), ExampleSingleValue("off"))),
      )
      SchemaClassifier.classifyField(s) must be(Some(FieldKind.InlineEnum(s)))
    }
  }

  "convert" must {

    "filter skipped schemas from union members" in {
      val schemas: ListMap[String, Schema] = ListMap(
        "FullSchema" -> Schema(
          `type` = Some(List(SchemaType.Object)),
          properties = ListMap("id" -> Schema(`type` = Some(List(SchemaType.String)))),
        ),
        "ExampleOnly" -> Schema(examples = Some(List(ExampleSingleValue("sample")))),
        "MyUnion" -> Schema(oneOf = List(
          Schema($ref = Some("#/components/schemas/FullSchema")),
          Schema($ref = Some("#/components/schemas/ExampleOnly")),
        )),
      )

      val classification = SchemaClassifier.classify(schemas)
      val result = new SchemaConverter(Map.empty, NamingConfig()).convert(classification)

      result.unions must have size 1
      result.unions.head.types.map(_.`type`) must be(Seq("full_schema"))
    }

    "SchemaConverter.convert: object schema with two properties produces a Model with correct field names and types" in {
      val schemas: ListMap[String, Schema] = ListMap(
        "Widget" -> Schema(
          `type` = Some(List(SchemaType.Object)),
          properties = ListMap(
            "id" -> Schema(`type` = Some(List(SchemaType.String))),
            "count" -> Schema(`type` = Some(List(SchemaType.Integer))),
          ),
        ),
      )
      val classification = SchemaClassifier.classify(schemas)
      val result = new SchemaConverter(Map.empty, NamingConfig()).convert(classification)

      result.models must have size 1
      val model = result.models.head
      model.name must be("widget")
      val fieldNames = model.fields.map(_.name)
      fieldNames must contain("id")
      fieldNames must contain("count")
      model.fields.find(_.name == "id").map(_.`type`) must be(Some("string"))
      model.fields.find(_.name == "count").map(_.`type`) must be(Some("integer"))
    }

    "SchemaConverter.convert: string enum schema produces an Enum with the right values" in {
      val schemas: ListMap[String, Schema] = ListMap(
        "Status" -> Schema(
          `type` = Some(List(SchemaType.String)),
          `enum` = Some(List(ExampleSingleValue("active"), ExampleSingleValue("inactive"))),
        ),
      )
      val classification = SchemaClassifier.classify(schemas)
      val result = new SchemaConverter(Map.empty, NamingConfig()).convert(classification)

      result.enums must have size 1
      val enumDef = result.enums.head
      enumDef.name must be("status")
      enumDef.values.map(_.name) must contain allOf ("active", "inactive")
    }

    "SchemaConverter.convert: field with unresolvable type is omitted" in {
      val schemas: ListMap[String, Schema] = ListMap(
        "Widget" -> Schema(
          `type` = Some(List(SchemaType.Object)),
          properties = ListMap(
            "name" -> Schema(`type` = Some(List(SchemaType.String))),
            "mystery" -> Schema(),
          ),
        ),
      )
      val classification = SchemaClassifier.classify(schemas)
      val result = new SchemaConverter(Map.empty, NamingConfig()).convert(classification)

      result.models must have size 1
      val model = result.models.head
      model.fields.map(_.name) must contain("name")
      model.fields.map(_.name) must not contain "mystery"
    }
  }
}
