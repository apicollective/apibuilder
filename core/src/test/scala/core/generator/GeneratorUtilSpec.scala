package core.generator

import org.scalatest.{ ShouldMatchers, FunSpec }

class GeneratorUtilSpec extends FunSpec with ShouldMatchers {

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
    GeneratorUtil.urlToMethodName("/memberships", "GET", "/memberships") should be("get")
    GeneratorUtil.urlToMethodName("/memberships", "POST", "/memberships") should be("post")
    GeneratorUtil.urlToMethodName("/memberships", "GET", "/memberships/:guid") should be("getByGuid")
    GeneratorUtil.urlToMethodName("/memberships", "POST", "/memberships/:guid/accept") should be("postAcceptByGuid")

    GeneratorUtil.urlToMethodName("/membership_requests", "GET", "/membership_requests") should be("get")
    GeneratorUtil.urlToMethodName("/membership_requests", "POST", "/membership_requests") should be("post")
    GeneratorUtil.urlToMethodName("/membership_requests", "GET", "/membership_requests/:guid") should be("getByGuid")

    GeneratorUtil.urlToMethodName("/membership-requests", "GET", "/membership-requests") should be("get")
    GeneratorUtil.urlToMethodName("/membership-requests", "POST", "/membership-requests") should be("post")
    GeneratorUtil.urlToMethodName("/membership-requests", "GET", "/membership-requests/:guid") should be("getByGuid")

    GeneratorUtil.urlToMethodName("/:key", "GET", "/:key") should be("getByKey")
  }

}
