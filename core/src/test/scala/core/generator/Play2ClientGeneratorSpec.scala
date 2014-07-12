package core.generator

import core.TestHelper
import org.scalatest.{ ShouldMatchers, FunSpec }

class Play2ClientGeneratorSpec extends FunSpec with ShouldMatchers {

  private val Path = "api/api.json"
  private lazy val service = TestHelper.parseFile(Path).serviceDescription.get

  it("errorTypeClass") {
    val ssd = new ScalaServiceDescription(service)
    val resource = ssd.resources.find(_.model.name == "Organization").get
    val operation = resource.operations.find(_.method == "POST").get
    val errorResponse = operation.responses.find(_.code == 409).get
    println("errorResponse:" + errorResponse.resultType)

    val target = """
case class ErrorsResponse(response: play.api.libs.ws.Response) extends Exception {

  lazy val errors: scala.collection.Seq[Error] = response.json.as[scala.collection.Seq[Error]]

}
""".trim
    Play2ClientGenerator.errorTypeClass(errorResponse).trim should be(target)
  }

}
