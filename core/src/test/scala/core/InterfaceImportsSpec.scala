package core

import cats.data.Validated.{Invalid, Valid}
import helpers.ApiJsonHelpers
import io.apibuilder.spec.v0.models.json._
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json

/**
 * Tests that you can use an interface from an imported service
 */
class InterfaceImportsSpec extends AnyFunSpec with Matchers with ApiJsonHelpers {

  private[this] val importedServiceFile: String = TestHelper.writeToTempFile(
    Json.toJson(
      toService(
        makeApiJson(
          name = "definitions",
          namespace = Some("definitions"),
          interfaces = Map("user" -> makeInterface())
        )
      )
    ).toString
  )

  it("models can declare an interface from an imported service") {
    def setup(importUris: Seq[String]) = {
      makeApiJson(
        name = "svc",
        imports = importUris.map { uri => makeImport(uri = uri) },
        models = Map("test" -> makeModelWithField(interfaces = Some(Seq("definitions.interfaces.user")))),
      )
    }

    TestHelper.expectSingleError(setup(Nil)) should equal(
      "Model[test] Interface[definitions.interfaces.user] not found"
    )

    setupValidApiJson(setup(Seq(s"file://$importedServiceFile"))).models.head.interfaces should equal(
      Seq("definitions.interfaces.user")
    )
  }

  // TODO: Ideally we can fetch the imported service and get the list of interface fields
  // like we do for a locally declared interface. We should make the treatment the same
  // for imported or local interfaces
}
