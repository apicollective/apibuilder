package core.generator

import core._
import org.scalatest.{ ShouldMatchers, FunSpec }

class Play2UtilSpec extends FunSpec with ShouldMatchers {

  private lazy val service = TestHelper.parseFile("reference-api/api.json").serviceDescription.get
  private lazy val ssd = new ScalaServiceDescription(service)

  private val play2Util = Play2Util(ScalaClientMethodConfigs.Play)

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
      val code = play2Util.queryParams(
        new ScalaOperation(
          ssd,
          new ScalaModel(ssd, model),
          operation,
          new ScalaResource(ssd, resource)
        )
      )
      code.get should equal("""val query = Seq(
  Some("q1" -> q1.toString),
  q2.map("q2" -> _.toString)
).flatten""")
    }
  }

  it("supports query parameters that contain lists") {
    val operation = ssd.resources.find(_.model.name == "Echo").get.operations.head
    val code = play2Util.queryParams(operation).get
    code should be("""
val query = Seq(
  foo.map("foo" -> _)
).flatten ++
  optionalMessages.map("optional_messages" -> _) ++
  requiredMessages.map("required_messages" -> _)
""".trim)
  }

  it("supports query parameters that ONLY have lists") {
    val operation = ssd.resources.find(_.model.name == "Echo").get.operations.find(_.path == "/echoes/arrays-only").get
    val code = play2Util.queryParams(operation).get
    code should be("""
val query = optionalMessages.map("optional_messages" -> _) ++
  requiredMessages.map("required_messages" -> _)
""".trim)
  }

  describe("with reference-api service") {
    lazy val service = TestHelper.parseFile(s"reference-api/api.json").serviceDescription.get

    it("supports optional seq  query parameters") {
      val operation = ssd.resources.find(_.model.name == "User").get.operations.find(op => op.method == "GET" && op.path == "/users").get

      TestHelper.assertEqualsFile(
        "core/src/test/resources/generators/play-2-route-util-reference-get-users.txt",
        play2Util.queryParams(operation).get
      )
    }

  }

}
