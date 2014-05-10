package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class SvcApiDocJson extends FunSpec with Matchers {

  private val Path = "svc/api.json"

  it("parses models") {
    val service = TestHelper.parseFile(Path).serviceDescription.get
    service.models.map(_.name).sorted.mkString(" ") should be("membership membership_request membership_request_review organization service user version")

    val user = service.models.find(_.name == "user").get
    user.fields.map(_.name).mkString(" ") should be("guid email name image_url")
    user.fields.find(_.name == "guid").get.datatype.name should be("uuid")
    user.fields.find(_.name == "email").get.datatype.name should be("string")
  }


  it("all POST operations return either a 201 or a 409") {
    val service = TestHelper.parseFile(Path).serviceDescription.get
    service.operations.filter(_.method == "POST").foreach { op =>
      if (op.responses.map(_.code).sorted != Seq(201, 409)) {
        fail("POST operation should return a 201 or a 409: " + op)
      }
     }
  }

}
