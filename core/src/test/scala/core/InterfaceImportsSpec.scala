package core

import io.apibuilder.spec.v0.models.json._
import org.scalatest.{FunSpec, Matchers}
import play.api.libs.json.Json

/**
 * Tests that you can use an interface from an imported service
 */
class InterfaceImportsSpec extends FunSpec with Matchers with helpers.ApiJsonHelpers {

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
      TestHelper.serviceValidator(
        makeApiJson(
          name = "svc",
          imports = importUris.map { uri => makeImport(uri = uri) },
          models = Map("test" -> makeModelWithField(interfaces = Some(Seq("definitions.interfaces.user")))),
        )
      )
    }

    setup(Nil).errors() should equal(
      Seq("Model[test] Interface[definitions.interfaces.user] not found")
    )

    val v = setup(Seq(s"file://$importedServiceFile"))
    v.errors() should equal(Nil)
    v.service().models.head.interfaces should equal(
      Seq("definitions.interfaces.user")
    )
  }

  // TODO: Ideally we can fetch the imported service and get the list of interface fields
  // like we do for a locally declared interface. We should make the treatment the same
  // for imported or local interfaces
}
