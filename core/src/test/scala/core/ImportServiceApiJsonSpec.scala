package core

import builder.OriginalValidator
import org.scalatest.{FunSpec, Matchers}

class ImportServiceApiJsonSpec extends FunSpec with Matchers with helpers.ApiJsonHelpers {

  describe("validation") {

    it("import uri is present") {
      val json =
        """{
        "name": "Import Shared",
        "apidoc": { "version": "0.9.6" },
        "info": {},
        "imports": [ { "foo": "bar" } ]
      }"""
      val validator = TestHelper.serviceValidatorFromApiJson(json)
      validator.errors().mkString(",") should be("Import Unrecognized element[foo],Import Missing uri")
    }

    it("import uri cannot be empty") {
      def testUri(uri: String) = {
        TestHelper.serviceValidator(
          makeApiJson(
            imports = Seq(makeImport(uri = uri))
          )
        )
      }

      testUri("  ").errors() should be(Seq("Import uri must be a non empty string"))
      testUri("foobar").errors() should be(Seq("URI[foobar] must start with http://, https://, or file://"))
      testUri("https://app.apibuilder.io/").errors() should be(Seq("URI[https://app.apibuilder.io/] cannot end with a '/'"))
    }

  }
}
