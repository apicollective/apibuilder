package db.generators

import db.Authorization
import helpers.ValidatedTestHelpers
import io.apibuilder.api.v0.models.{GeneratorForm, GeneratorService, GeneratorServiceForm}
import io.apibuilder.generator.v0.mock.Factories
import io.apibuilder.generator.v0.models.Generator

import java.util.UUID

trait GeneratorHelpers extends db.Helpers with ValidatedTestHelpers {

  def createGeneratorService(
    form: GeneratorServiceForm = makeGeneratorServiceForm()
  ): InternalGeneratorService = {
    expectValid {
      servicesDao.create(testUser, form)
    }
  }

  def makeGeneratorServiceForm(
    uri: String = s"https://test.generator.${UUID.randomUUID}"
  ): GeneratorServiceForm = {
    GeneratorServiceForm(
      uri = uri
    )
  }

  def createGenerator(
    service: InternalGeneratorService = createGeneratorService()
  ): InternalGenerator = {
    val form = createGeneratorForm(service = service)

    expectValid {
      generatorsDao.upsert(testUser, form)
    }
  }

  def createGeneratorForm(
    service: InternalGeneratorService = createGeneratorService(),
    generator: Generator = makeGenerator()
  ): GeneratorForm = {
    GeneratorForm(
      serviceGuid = service.guid,
      generator = generator
    )
  }

  def makeGenerator(attributes: Seq[String] = Nil): Generator = {
    val value = UUID.randomUUID.toString.toLowerCase
    Factories.makeGenerator().copy(
      key = "test_" + value,
      name = "Test " + value,
      attributes = attributes
    )
  }

}
