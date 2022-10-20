package builder.api_json.upgrades

import helpers.ApiJsonHelpers
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ApiDocRemovedFromSpecSpec extends AnyFunSpec with Matchers with ApiJsonHelpers {
  it("rewrites") {
    makeApiJson()

  }
}