package models

import lib.Primitives
import com.gilt.apidocspec.models._
import core.ServiceBuilder
import org.scalatest.{ ShouldMatchers, FunSpec }

class RubyClientPrimitiveObjectSpec extends FunSpec with ShouldMatchers {

  describe("for a field with an object field") {

    val baseJson = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc Test",

      "models": {
        "content": {
          "fields": [
            { "name": "data", "type": "%s" }
          ]
        }
      }
    }
    """

    def sd(typeString: String): Service = {
      ServiceBuilder(baseJson.format(typeString))
    }

    def model(typeString: String): Model = {
      sd(typeString).models.head
    }

    def dataField(typeString: String): Field = {
      model(typeString).fields.head
    }

    it("singleton object") {
      dataField("object").`type` should be(TypeInstance(Container.Singleton, Type(TypeKind.Primitive, Primitives.Object.toString)))
    }

    it("list object") {
      dataField("[object]").`type` should be(TypeInstance(Container.List, Type(TypeKind.Primitive, Primitives.Object.toString)))
    }

    it("map object") {
      dataField("map[object]").`type` should be(TypeInstance(Container.Map, Type(TypeKind.Primitive, Primitives.Object.toString)))
    }

    describe("generates valid models") {

      it("singleton") {
        val code = RubyClientGenerator(sd("object")).generateModel(model("object"))
        TestHelper.assertEqualsFile("test/resources/generators/ruby-client-primitive-object-singleton.txt", code)
      }

      it("list") {
        val code = RubyClientGenerator(sd("object")).generateModel(model("[object]"))
        TestHelper.assertEqualsFile("test/resources/generators/ruby-client-primitive-object-list.txt", code)
      }

      it("map") {
        val code = RubyClientGenerator(sd("object")).generateModel(model("map[object]"))
        TestHelper.assertEqualsFile("test/resources/generators/ruby-client-primitive-object-map.txt", code)
      }

    }

  }

  describe("for a response with an object field") {

    val baseJson = """
    {
      "base_url": "http://localhost:9000",
      "name": "Api Doc Test",

      "models": {
        "content": {
          "fields": [
            { "name": "id", "type": "long" }
          ]
        }
      },

      "resources": {
        "content": {
          "operations": [
            {
              "method": "GET",
              "path": "/data",
              "responses": {
                "200": { "type": "%s" }
              }
            }
          ]
        }
      }

    }
    """

    def sd(typeString: String): Service = {
      ServiceBuilder(baseJson.format(typeString))
    }

    def operation(typeString: String): Operation = {
      sd(typeString).resources.head.operations.head
    }

    def response(typeString: String): Response = {
      operation(typeString).responses.head
    }


    it("singleton object") {
      response("object").`type` should be(TypeInstance(Container.Singleton, Type(TypeKind.Primitive, Primitives.Object.toString)))
    }

    it("list object") {
      response("[object]").`type` should be(TypeInstance(Container.List, Type(TypeKind.Primitive, Primitives.Object.toString)))
    }

    it("map object") {
      response("map[object]").`type` should be(TypeInstance(Container.Map, Type(TypeKind.Primitive, Primitives.Object.toString)))
    }

    describe("generates valid response code") {

      it("singleton") {
        val code = RubyClientGenerator(sd("object")).generateResponses(operation("object"))
        TestHelper.assertEqualsFile("test/resources/generators/ruby-client-primitive-object-response-singleton.txt", code)
      }

      it("list") {
        val code = RubyClientGenerator(sd("object")).generateResponses(operation("[object]"))
        TestHelper.assertEqualsFile("test/resources/generators/ruby-client-primitive-object-response-list.txt", code)
      }

      it("map") {
        val code = RubyClientGenerator(sd("object")).generateResponses(operation("map[object]"))
        TestHelper.assertEqualsFile("test/resources/generators/ruby-client-primitive-object-response-map.txt", code)
      }

    }
  }

}
