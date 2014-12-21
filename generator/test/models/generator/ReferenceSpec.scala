package generator

import models.TestHelper
import core.ServiceValidator
import org.scalatest.{ FunSpec, Matchers }

class ReferenceSpec extends FunSpec with Matchers {

  lazy val service = TestHelper.parseFile(s"reference-api/api.json").serviceDescription.get
  lazy val ssd = new ScalaService(service)

  it("user case classes") {
    val model = ssd.models.find(_.name == "User").getOrElse { sys.error("Failed to find user model") }
    val code = ScalaCaseClasses.generateCaseClass(model)
    TestHelper.assertEqualsFile("test/resources/generators/reference-spec-user-case-class.txt", code)
  }

  it("member case classes") {
    val model = ssd.models.find(_.name == "Member").getOrElse { sys.error("Failed to find member model") }
    val code = ScalaCaseClasses.generateCaseClass(model)
    TestHelper.assertEqualsFile("test/resources/generators/reference-spec-member-case-class.txt", code)
  }

}


