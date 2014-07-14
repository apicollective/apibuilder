package core.generator

import core._
import org.scalatest.{ ShouldMatchers, FunSpec }

class Play2UtilSpec extends FunSpec with ShouldMatchers {

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
    val operation = new Operation(model, "GET", "models", None, Seq(q1, q2), Nil)
    val resource = new Resource(model, "models", Seq(operation))

    it("should handle required and non-required params") {
      val Some(code) = Play2Util.queryParams(
        new ScalaOperation(
          new ScalaModel(model),
          operation,
          new ScalaResource("test", resource)
        )
      )
      code should equal("""val query = Seq(
  Some(q1).map("q1" -> _.toString),
  q2.map("q2" -> _.toString)
).flatten""")
    }
  }
}
