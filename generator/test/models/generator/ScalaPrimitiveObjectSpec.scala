package generator

import lib.Primitives
import com.gilt.apidocspec.models._
import models.TestHelper
import core.ServiceBuilder
import org.scalatest.{ ShouldMatchers, FunSpec }

class ScalaPrimitiveObjectSpec extends FunSpec with ShouldMatchers {

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

    def ssd(typeString: String): ScalaService = {
      new ScalaService(ServiceBuilder(baseJson.format(typeString)))
    }

    def dataField(typeString: String): ScalaField = {
      ssd(typeString).models.head.fields.head
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

    describe("generates valid case classes") {

      it("singleton") {
        val code = ScalaCaseClasses.generate(ssd("object"), addHeader = false)
        TestHelper.assertEqualsFile("test/resources/generators/scala-primitive-object-singleton.txt", code)
      }

      it("list") {
        val code = ScalaCaseClasses.generate(ssd("[object]"), addHeader = false)
        TestHelper.assertEqualsFile("test/resources/generators/scala-primitive-object-list.txt", code)
      }

      it("map") {
        val code = ScalaCaseClasses.generate(ssd("map[object]"), addHeader = false)
        TestHelper.assertEqualsFile("test/resources/generators/scala-primitive-object-map.txt", code)
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

    def ssd(typeString: String): ScalaService = {
      new ScalaService(ServiceBuilder(baseJson.format(typeString)))
    }

    def operation(typeString: String): ScalaOperation = {
      ssd(typeString).resources.head.operations.head
    }

    def response(typeString: String): ScalaResponse = {
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
        val generator = new ScalaClientMethodGenerator(ScalaClientMethodConfigs.Play23, ssd("object"))
        TestHelper.assertEqualsFile("test/resources/generators/scala-primitive-object-response-singleton.txt", generator.objects)
      }

      it("list") {
        val generator = new ScalaClientMethodGenerator(ScalaClientMethodConfigs.Play23, ssd("[object]"))
        TestHelper.assertEqualsFile("test/resources/generators/scala-primitive-object-response-list.txt", generator.objects)
      }

      it("map") {
        val generator = new ScalaClientMethodGenerator(ScalaClientMethodConfigs.Play23, ssd("map[object]"))
        TestHelper.assertEqualsFile("test/resources/generators/scala-primitive-object-response-map.txt", generator.objects)

      }

    }

  }

}
