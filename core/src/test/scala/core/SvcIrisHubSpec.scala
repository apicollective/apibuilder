package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class SvcIrisHubSpec extends FunSpec with Matchers {

  val Filenames = Seq("svc-iris-hub-0-0-1.json")

  private def readFile(filename: String): String = {
    val path = s"core/src/test/files/${filename}"
    scala.io.Source.fromFile(path).getLines.mkString("\n")
  }

  private def parseFile(filename: String): ServiceDescriptionValidator = {
    val contents = readFile(filename)
    ServiceDescriptionValidator(contents)
  }

  it("should parse valid json") {
    Filenames.foreach { name =>
      val validator = parseFile(name)
      if (!validator.isValid) {
        fail(s"Error parsing json file ${name}:\n  - " + validator.errors.mkString("\n  - "))
      }
    }
  }

  it("parses models") {
    val service = parseFile("svc-iris-hub-0-0-1.json").serviceDescription.get
    service.models.map(_.name).sorted.mkString(" ") should be("address agreement error_message item planned_shipment purchase " +
                                                              "receipt shipment_request shipment_request_item shipment_schedule " +
                                                              "term vendor")

    val item = service.models.find(_.name == "item").get
    item.fields.map(_.name).mkString(" ") should be("guid vendor number quantity data")
    item.fields.find(_.name == "number").get.datatype.name should be("string")
  }

  it("parses operations") {
    val service = parseFile("svc-iris-hub-0-0-1.json").serviceDescription.get
    service.models.map(_.name).sorted.mkString(" ") should be("address agreement error_message item planned_shipment purchase " +
                                                              "receipt shipment_request shipment_request_item shipment_schedule " +
                                                              "term vendor")

    val operations = service.operations.filter(_.model.name == "item")

    val gets = operations.filter(op => op.method == "GET" && op.path == None)
    gets.size should be(1)
    gets.head.parameters.map(_.name).mkString(" ") should be("vendor_guid agreement_guid number limit offset")
    val response = gets.head.responses.head
    response.code should be(200)
    response.datatype should be("item")
    response.multiple should be(true)

    val getsByGuid = operations.filter(op => op.method == "GET" && op.path == Some("/:guid"))
    getsByGuid.size should be(1)
    getsByGuid.head.parameters.map(_.name).mkString(" ") should be("guid")
    getsByGuid.head.responses.map(_.code) should be(Seq(200))

    val deletes = operations.filter(op => op.method == "DELETE" )
    deletes.size should be(1)
    deletes.head.parameters.map(_.name).mkString(" ") should be("guid")
    deletes.head.responses.map(_.code) should be(Seq(204))
  }

}
