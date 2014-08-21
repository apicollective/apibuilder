package core.generator

import core.Datatype.IntegerType
import core._
import org.scalatest._

class ScalaOperationSpec extends FunSpec with ShouldMatchers {

  private lazy val service = TestHelper.parseFile("reference-api/api.json").serviceDescription.get

  val q1 = new Parameter(
    "q1",
    new PrimitiveParameterType(Datatype.DoubleType),
    ParameterLocation.Query,
    None, false, false, None, None, None, None)

  it("models as a parameter in the body should use capitalize") {
    val body = new ModelBody("model")
    val model = new Model("model", "models", None, Nil)
    val operation = new Operation(model, "GET", "models", None, Some(body), Seq(q1), Nil)
    val resource = new Resource(model, "models", Seq(operation))

    val scalaOperation = new ScalaOperation(
      service,
      new ScalaModel(service, model),
      operation,
      new ScalaResource(service, "test", resource))

    scalaOperation.argList shouldEqual Some("model: test.models.Model, \n  q1: scala.Option[Double] = None\n")

  }

  it("primitive type as a parameter in the body should not use capitalize") {
    val body = new PrimitiveBody(IntegerType)
    val model = new Model("model", "models", None, Nil)
    val operation = new Operation(model, "GET", "models", None, Some(body), Seq(q1), Nil)
    val resource = new Resource(model, "models", Seq(operation))

    val scalaOperation = new ScalaOperation(
      service,
      new ScalaModel(service, model),
      operation,
      new ScalaResource(service, "test", resource))

    scalaOperation.argList shouldEqual Some("value: Int, \n  q1: scala.Option[Double] = None\n")

  }

}
