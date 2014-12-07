package controllers

import com.gilt.apidoc.models._
import db._
import java.util.UUID

import play.api.test._
import play.api.test.Helpers._
import org.scalatestplus.play._

abstract class BaseSpec extends PlaySpec with OneServerPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit override lazy val port = 9010
  implicit override lazy val app: FakeApplication = FakeApplication()

  val UserOtto = User(
    guid = UUID.fromString("9472ae70-30c2-012c-8f71-0015177442e6"),
    email = "otto@gilt.com",
    name = Some("Otto Mann")
  )

  val client = new com.gilt.usersegment.Client(s"http://localhost:$port")

  def createOrganization(
    form: OrganizationForm = createOrganizationForm()
  ): Organization = {
    await(client.organizations.post(form))
  }

  def createOrganizationForm() = OrganizationForm(
    key = "z-test-org-" + UUID.randomUUID.toString
  )

}
