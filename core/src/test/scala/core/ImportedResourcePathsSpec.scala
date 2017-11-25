package core

import org.scalatest.{FunSpec, Matchers}

class ImportedResourcePathsSpec extends FunSpec with Matchers {

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
    fetcher.add(uri, TestHelper.serviceValidatorFromApiJson(common).service)

    val validator = TestHelper.serviceValidatorFromApiJson(user, fetcher = fetcher)
    validator.errors should be(Nil)

    val userResource = validator.service().resources.head
    userResource.operations.map(_.path) should be(Seq("/users"))
  }

}
