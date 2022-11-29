package core

import helpers.ValidatedTestHelpers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ImportedResourcePathsSpec extends AnyFunSpec with Matchers with ValidatedTestHelpers {

  it("generates appropriate path for resources from imported models") {
    val common = """
    {
      "name": "common",
      "namespace": "test.common.v0",
      "models": {
        "user": {
          "fields": [
            { "name": "id", "type": "string" }
          ]
        }
      }
    }
    """

    val uri = "http://localhost/test/common/0.0.1/service.json"
    val user = s"""
    {
      "name": "user",
      "imports": [ { "uri": "$uri" } ],

      "resources": {
        "test.common.v0.models.user": {
          "operations": [
            { "method": "DELETE" }
          ]
        }
      }
    }
    """

    val fetcher = MockServiceFetcher()
    fetcher.add(uri, TestHelper.serviceValidatorFromApiJson(common).service())

    expectValid {
      TestHelper.serviceValidatorFromApiJson(user, fetcher = fetcher).validate()
    }.resources.head.operations.map(_.path) should be(Seq("/users"))
  }

}
