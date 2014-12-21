package generator

import models.TestHelper
import lib.Primitives
import com.gilt.apidocspec.models._
import core._
import org.scalatest._

class ScalaOperationSpec extends FunSpec with ShouldMatchers {

  private lazy val service = TestHelper.parseFile("reference-api/api.json").serviceDescription.get
  private lazy val ssd = new ScalaService(service)

  val q1 = new Parameter(
    "q1",
    TypeInstance(Container.Singleton, Type(TypeKind.Primitive, Primitives.Double.toString)),
    ParameterLocation.Query,
    None, false, None, None, None, None)

  it("models as a parameter in the body should use capitalize") {
    val body = com.gilt.apidocspec.models.Body(TypeInstance(Container.Singleton, Type(TypeKind.Model, "model")))
    val model = new Model("model", "models", None, Nil)
    val operation = new Operation(model, "GET", "models", None, Some(body), Seq(q1), Nil)
    val resource = new Resource(model, "models", Seq(operation))

    val scalaOperation = new ScalaOperation(
      ssd,
      new ScalaModel(ssd, model),
      operation,
      new ScalaResource(ssd, resource))

    scalaOperation.argList shouldEqual Some("model: apidocreferenceapi.models.Model,\n  q1: scala.Option[Double] = None\n")
  }

  it("array of models as a parameter in the body should use capitalize") {
    val body = com.gilt.apidocspec.models.Body(TypeInstance(Container.List, Type(TypeKind.Model, "model")))
    val model = new Model("model", "models", None, Nil)
    val operation = new Operation(model, "GET", "models", None, Some(body), Seq(q1), Nil)
    val resource = new Resource(model, "models", Seq(operation))

    val scalaOperation = new ScalaOperation(
      ssd,
      new ScalaModel(ssd, model),
      operation,
      new ScalaResource(ssd, resource))

    scalaOperation.argList shouldEqual Some("models: Seq[apidocreferenceapi.models.Model],\n  q1: scala.Option[Double] = None\n")
  }

  it("primitive type as a parameter in the body should not use capitalize") {
    val body = com.gilt.apidocspec.models.Body(TypeInstance(Container.Singleton, Type(TypeKind.Primitive, Primitives.Integer.toString)))
    val model = new Model("model", "models", None, Nil)
    val operation = new Operation(model, "GET", "models", None, Some(body), Seq(q1), Nil)
    val resource = new Resource(model, "models", Seq(operation))

    val scalaOperation = new ScalaOperation(
      ssd,
      new ScalaModel(ssd, model),
      operation,
      new ScalaResource(ssd, resource))

    scalaOperation.argList shouldEqual Some("value: Int,\n  q1: scala.Option[Double] = None\n")
  }

  it("array of primitive types as a parameter in the body should not use capitalize") {
    val body = com.gilt.apidocspec.models.Body(TypeInstance(Container.List, Type(TypeKind.Primitive, Primitives.Integer.toString)))
    val model = new Model("model", "models", None, Nil)
    val operation = new Operation(model, "GET", "models", None, Some(body), Seq(q1), Nil)
    val resource = new Resource(model, "models", Seq(operation))

    val scalaOperation = new ScalaOperation(
      ssd,
      new ScalaModel(ssd, model),
      operation,
      new ScalaResource(ssd, resource)
    )

    scalaOperation.argList shouldEqual Some("values: Seq[Int],\n  q1: scala.Option[Double] = None\n")

  }

}
