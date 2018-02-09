package controllers

import io.apibuilder.api.v0.models.{GeneratorService, GeneratorServiceForm}
import java.util.UUID

import org.scalatestplus.play.OneServerPerSuite
import play.api.test._

class GeneratorServicesSpec extends PlaySpecification with MockClient with OneServerPerSuite {

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

    expectNotFound {
      client.generatorServices.getByGuid(UUID.randomUUID)
    }
  }

  "DELETE /generator_services/:guid" in new WithServer(port=defaultPort) {
    val service = createGeneratorService()

    await(client.generatorServices.deleteByGuid(service.guid)) must beEqualTo(())
    expectNotFound {
      client.generatorServices.getByGuid(service.guid)
    }
    expectNotFound {
      client.generatorServices.deleteByGuid(service.guid)
    }

  }

}
