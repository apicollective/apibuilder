package controllers

import db.Authorization
import db.generators.ServicesDao
import io.apibuilder.api.v0.models.{GeneratorService, GeneratorServiceForm}
import io.apibuilder.api.v0.errors.UnitResponse
import java.util.UUID

import play.api.test._
import play.api.test.Helpers._
import scala.util.{Failure, Success, Try}

class GeneratorServicesSpec extends PlaySpecification with MockClient {

  import scala.concurrent.ExecutionContext.Implicits.global

// GET        /generator_services/:guid                           controllers.GeneratorServices.getByGuid(guid: _root_.java.util.UUID)
// DELETE     /generator_services/:guid                           controllers.GeneratorServices.deleteByGuid(guid: _root_.java.util.UUID)

  def createGeneratorService(
    form: GeneratorServiceForm = createGeneratorServiceForm()
  ): GeneratorService = {
    // TODO: Switch to REST API. But first need to resolve dependency
    // on fetching the list of generators from the service.
    // await(client.generatorServices.post(form))
    servicesDao.create(TestUser, form)
  }

  def createGeneratorServiceForm(
    uri: String = s"http://${UUID.randomUUID}.com"
  ): GeneratorServiceForm = {
    GeneratorServiceForm(
      uri = uri
    )
  }

  "POST /generator_services" in new WithServer(port=defaultPort) {
    val form = createGeneratorServiceForm()
    val service = createGeneratorService(form)
    service.uri must beEqualTo(form.uri)
  }

  "GET /generator_services/:guid" in new WithServer(port=defaultPort) {
    val service = createGeneratorService()
    await(client.generatorServices.getByGuid(service.guid)) must beEqualTo(service)
    intercept[UnitResponse] {
      await(client.generatorServices.getByGuid(UUID.randomUUID))
    }
  }

  "DELETE /generator_services/:guid" in new WithServer(port=defaultPort) {
    val service = createGeneratorService()

    await(client.generatorServices.deleteByGuid(service.guid)) must beEqualTo(())
    intercept[UnitResponse] {
      await(client.generatorServices.getByGuid(service.guid))
    }
    intercept[UnitResponse] {
      await(client.generatorServices.deleteByGuid(service.guid))
    }

  }

}
