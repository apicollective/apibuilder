package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class SvcIrisHubSpec extends FunSpec with Matchers {

  val Filenames = Seq("svc-iris-hub-0-0-1.json")
  val Dir = "core/src/test/files"

  it("should parse valid json") {
    Filenames.foreach { name =>
      val validator = TestHelper.parseFile(s"${Dir}/${name}")
      if (!validator.isValid) {
        fail(s"Error parsing json file ${name}:\n  - " + validator.errors.mkString("\n  - "))
      }
    }
  }

  it("parses models") {
    val service = TestHelper.parseFile(s"${Dir}/svc-iris-hub-0-0-1.json").serviceDescription.get
    service.models.map(_.name).sorted.mkString(" ") should be("address agreement error_message item planned_shipment purchase " +
                                                              "receipt shipment_request shipment_request_item shipment_schedule " +
                                                              "term user vendor vendor_tag")

    val item = service.models.find(_.name == "item").get
    item.fields.map(_.name).mkString(" ") should be("guid vendor number quantity data")
    item.fields.find(_.name == "number").get.fieldtype.asInstanceOf[PrimitiveFieldType].datatype.name should be("string")
  }

  it("parses operations") {
    val service = TestHelper.parseFile(s"${Dir}/svc-iris-hub-0-0-1.json").serviceDescription.get
    val operations = service.operations.filter(_.model.name == "item")

    val gets = operations.filter(op => op.method == "GET" && op.path == "/items")
    gets.size should be(1)
    gets.head.parameters.map(_.name).mkString(" ") should be("vendor_guid agreement_guid number limit offset")
    val response = gets.head.responses.head
    response.code should be(200)
    response.datatype should be("item")
    response.multiple should be(true)

    val getsByGuid = operations.filter(op => op.method == "GET" && op.path == "/items/:guid")
    getsByGuid.size should be(1)
    getsByGuid.head.parameters.map(_.name).mkString(" ") should be("guid")
    getsByGuid.head.responses.map(_.code) should be(Seq(200))

    val deletes = operations.filter(op => op.method == "DELETE" )
    deletes.size should be(1)
    deletes.head.parameters.map(_.name).mkString(" ") should be("guid")
    deletes.head.responses.map(_.code) should be(Seq(204))
  }

  it("all POST operations return either a 201 or a 409") {
    val service = TestHelper.parseFile(s"${Dir}/svc-iris-hub-0-0-1.json").serviceDescription.get
    service.operations.filter(_.method == "POST").foreach { op =>
      if (op.responses.map(_.code).sorted != Seq(201, 409)) {
        fail("POST operation should return a 201 or a 409: " + op)
      }
     }
  }

}
