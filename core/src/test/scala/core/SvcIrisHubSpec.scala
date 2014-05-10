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

}
