package core.generator

import org.scalatest.{ ShouldMatchers, FunSpec }

class GeneratorUtilSpec extends FunSpec with ShouldMatchers {

  it("isJsonDocumentMethod") {
    GeneratorUtil.isJsonDocumentMethod("GET") should be(false)
    GeneratorUtil.isJsonDocumentMethod("POST") should be(true)
    GeneratorUtil.isJsonDocumentMethod("PUT") should be(true)
    GeneratorUtil.isJsonDocumentMethod("PATCH") should be(true)
  }

  it("formatComment") {
    GeneratorUtil.formatComment("test") should be("# test")
    GeneratorUtil.formatComment("test this") should be("# test this")

    val source = "Search all users. Results are always paginated. You must specify at least 1 parameter"
    val target = """
# Search all users. Results are always paginated. You must specify at least 1
# parameter
""".trim
    GeneratorUtil.formatComment(source) should be(target)
  }

  it("urlToMethodName") {
    GeneratorUtil.urlToMethodName("Memberships", "GET", "/memberships") should be("get")
    GeneratorUtil.urlToMethodName("Memberships", "POST", "/memberships") should be("post")
    GeneratorUtil.urlToMethodName("Memberships", "GET", "/memberships/:guid") should be("getByGuid")
    GeneratorUtil.urlToMethodName("Memberships", "POST", "/memberships/:guid/accept") should be("postAcceptByGuid")

    GeneratorUtil.urlToMethodName("MembershipRequests", "GET", "/membership_requests") should be("get")
    GeneratorUtil.urlToMethodName("MembershipRequests", "POST", "/membership_requests") should be("post")
    GeneratorUtil.urlToMethodName("MembershipRequests", "GET", "/membership_requests/:guid") should be("getByGuid")
  }

}
