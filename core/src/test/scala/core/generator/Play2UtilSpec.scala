package core.generator

import core._
import org.scalatest.{ ShouldMatchers, FunSpec }

class Play2UtilSpec extends FunSpec with ShouldMatchers {

  private lazy val service = TestHelper.parseFile("reference-api/api.json").serviceDescription.get

  describe("queryParams") {
    val model = new Model("model", "models", None, Nil)
    val q1 = new Parameter(
      "q1",
      new PrimitiveParameterType(Datatype.DoubleType),
      ParameterLocation.Query,
      None, true, false, None, None, None, None
    )
    val q2 = new Parameter(
      "q2",
      new PrimitiveParameterType(Datatype.DoubleType),
      ParameterLocation.Query,
      None, false, false, None, None, None, None
    )
    val operation = new Operation(model, "GET", "models", None, None, Seq(q1, q2), Nil)
    val resource = new Resource(model, "models", Seq(operation))

    it("should handle required and non-required params") {
      val code = Play2Util.queryParams(
        new ScalaOperation(
          service,
          new ScalaModel(service, model),
          operation,
          new ScalaResource(service, "test", resource)
        )
      )
      code.get should equal("""val query = Seq(
  Some("q1" -> q1.toString),
  q2.map("q2" -> _.toString)
).flatten""")
    }
  }

  it("supports query parameters that contain lists") {
    val ssd = new ScalaServiceDescription(service)
    val operation = ssd.resources.find(_.model.name == "Echo").get.operations.head
    val code = Play2Util.queryParams(operation).get
    code should be("""
val query = Seq(
  foo.map("foo" -> _)
).flatten ++
  optionalMessages.getOrElse(Seq.empty).map("optional_messages" -> _) ++
  requiredMessages.map("required_messages" -> _)
""".trim)
  }

  it("supports query parameters that ONLY have lists") {
    val ssd = new ScalaServiceDescription(service)
    val operation = ssd.resources.find(_.model.name == "Echo").get.operations.find(_.path == "/echoes/arrays-only").get
    val code = Play2Util.queryParams(operation).get
    code should be("""
val query = optionalMessages.getOrElse(Seq.empty).map("optional_messages" -> _) ++
  requiredMessages.map("required_messages" -> _)
""".trim)
  }

}
