package core

import helpers.ValidatedTestHelpers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ImportServiceApiJsonSpec extends AnyFunSpec with Matchers with helpers.ApiJsonHelpers with ValidatedTestHelpers {

  describe("validation") {

    it("import uri is present") {
      val json =
        """{
        "name": "Import Shared",
        "apidoc": { "version": "0.9.6" },
        "info": {},
        "imports": [ { "foo": "bar" } ]
      }"""
      expectInvalid {
        TestHelper.serviceValidatorFromApiJson(json)
      }.mkString(",") should be("Import Unrecognized element[foo],Import Missing uri")
    }

    it("import uri cannot be empty") {
      def testUri(uri: String) = {
        expectInvalid {
          TestHelper.serviceValidator(
            makeApiJson(
              imports = Seq(makeImport(uri = uri))
            )
          )
        }
      }

      testUri("  ") should be(Seq("Import uri must be a non empty string"))
      testUri("foobar") should be(Seq("URI[foobar] must start with http://, https://, or file://"))
      testUri("https://app.apibuilder.io/") should be(Seq("URI[https://app.apibuilder.io/] cannot end with a '/'"))
    }

  }
}
