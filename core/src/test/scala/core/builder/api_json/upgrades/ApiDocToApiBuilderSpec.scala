package builder.api_json.upgrades

import helpers.ApiJsonHelpers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ApiDocToApiBuilderSpec extends AnyFunSpec with Matchers with ApiJsonHelpers {
  it("rewrites") {
    makeApiJson()

  }
}