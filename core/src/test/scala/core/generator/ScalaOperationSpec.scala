package core.generator

import core.Datatype.IntegerType
import core._
import org.scalatest._

class ScalaOperationSpec extends FunSpec with ShouldMatchers {

  private lazy val service = TestHelper.parseFile("reference-api/api.json").serviceDescription.get
  private lazy val ssd = new ScalaServiceDescription(service)

  val q1 = new Parameter(
    "q1",
    new PrimitiveParameterType(Datatype.DoubleType),
    ParameterLocation.Query,
    None, false, false, None, None, None, None)

  it("models as a parameter in the body should use capitalize") {
    val body = new ModelBody("model", false)
    val model = new Model("model", "models", None, Nil)
    val operation = new Operation(model, "GET", "models", None, Some(body), Seq(q1), Nil)
    val resource = new Resource(model, "models", Seq(operation))

    val scalaOperation = new ScalaOperation(
      ssd,
      new ScalaModel(ssd, model),
      operation,
      new ScalaResource(ssd, resource))

    scalaOperation.argList shouldEqual Some("model: referenceapi.models.Model, \n  q1: scala.Option[Double] = None\n")
  }

  it("array of models as a parameter in the body should use capitalize") {
    val body = new ModelBody("model", true)
    val model = new Model("model", "models", None, Nil)
    val operation = new Operation(model, "GET", "models", None, Some(body), Seq(q1), Nil)
    val resource = new Resource(model, "models", Seq(operation))

    val scalaOperation = new ScalaOperation(
      ssd,
      new ScalaModel(ssd, model),
      operation,
      new ScalaResource(ssd, resource))

    scalaOperation.argList shouldEqual Some("models: scala.collection.Seq[referenceapi.models.Model], \n  q1: scala.Option[Double] = None\n")
  }

  it("primitive type as a parameter in the body should not use capitalize") {
    val body = new PrimitiveBody(IntegerType, false)
    val model = new Model("model", "models", None, Nil)
    val operation = new Operation(model, "GET", "models", None, Some(body), Seq(q1), Nil)
    val resource = new Resource(model, "models", Seq(operation))

    val scalaOperation = new ScalaOperation(
      ssd,
      new ScalaModel(ssd, model),
      operation,
      new ScalaResource(ssd, resource))

    scalaOperation.argList shouldEqual Some("value: Int, \n  q1: scala.Option[Double] = None\n")
  }

  it("array of primitive types as a parameter in the body should not use capitalize") {
    val body = new PrimitiveBody(IntegerType, true)
    val model = new Model("model", "models", None, Nil)
    val operation = new Operation(model, "GET", "models", None, Some(body), Seq(q1), Nil)
    val resource = new Resource(model, "models", Seq(operation))

    val scalaOperation = new ScalaOperation(
      ssd,
      new ScalaModel(ssd, model),
      operation,
      new ScalaResource(ssd, resource)
    )

    scalaOperation.argList shouldEqual Some("values: scala.collection.Seq[Int], \n  q1: scala.Option[Double] = None\n")

  }

}
