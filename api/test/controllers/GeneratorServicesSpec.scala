package controllers

import io.apibuilder.api.v0.models.{GeneratorService, GeneratorServiceForm}
import java.util.UUID

import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.PlaySpec

class GeneratorServicesSpec extends PlaySpec with MockClient with GuiceOneServerPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

// GET        /generator_services/:guid                           controllers.GeneratorServices.getByGuid(guid: _root_.java.util.UUID)
// DELETE     /generator_services/:guid                           controllers.GeneratorServices.deleteByGuid(guid: _root_.java.util.UUID)

  def createGeneratorService(
    form: GeneratorServiceForm = createGeneratorServiceForm()
  ): GeneratorService = {
    // TODO: Switch to REST API. But first need to resolve dependency
    // on fetching the list of generators from the service.
    // await(client.generatorServices.post(form))
    servicesDao.create(testUser, form)
  }

  def createGeneratorServiceForm(
    uri: String = s"http://${UUID.randomUUID}.com"
  ): GeneratorServiceForm = {
    GeneratorServiceForm(
      uri = uri
    )
  }

  "POST /generator_services" in {
    val form = createGeneratorServiceForm()
    val service = createGeneratorService(form)
    service.uri must equal(form.uri)
  }

  "GET /generator_services/:guid" in {
    val service = createGeneratorService()
    await(client.generatorServices.getByGuid(service.guid)) must equal(service)

    expectNotFound {
      client.generatorServices.getByGuid(UUID.randomUUID)
    }
  }

  "DELETE /generator_services/:guid" in {
    val service = createGeneratorService()

    await(client.generatorServices.deleteByGuid(service.guid)) must equal(())
    expectNotFound {
      client.generatorServices.getByGuid(service.guid)
    }
    expectNotFound {
      client.generatorServices.deleteByGuid(service.guid)
    }

  }

}
