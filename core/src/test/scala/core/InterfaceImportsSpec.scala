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
          namespace = Some("definitions"),
          interfaces = Map("user" -> makeInterface())
        )
      )
    ).toString
  )

  it("models inherit fields") {
    def setup(importUris: Seq[String]) = {
      TestHelper.serviceValidator(
        makeApiJson(
          imports = importUris.map { uri => makeImport(uri = uri) },
          models = Map("test" -> makeModelWithField(interfaces = Some(Seq("definitions.user")))),
        )
      )
    }

    setup(Nil).errors() should equal(
      Seq("TODO")
    )

    val v = setup(Seq(s"file://$importedServiceFile"))
    v.errors() should equal(Nil)
  }

}
