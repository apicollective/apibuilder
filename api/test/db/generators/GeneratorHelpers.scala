package db.generators

import db.Authorization
import io.apibuilder.api.v0.models.{GeneratorForm, GeneratorService, GeneratorServiceForm, GeneratorWithService}
import io.apibuilder.generator.v0.models.Generator
import java.util.UUID

trait GeneratorHelpers extends db.Helpers {

  def createGeneratorService(
    form: GeneratorServiceForm = createGeneratorServiceForm()
  ): GeneratorService = {
    servicesDao.create(testUser, form)
  }

  def createGeneratorServiceForm(
    uri: String = s"http://test.generator.${UUID.randomUUID}"
  ): GeneratorServiceForm = {
    GeneratorServiceForm(
      uri = uri
    )
  }

  def createGenerator(
    service: GeneratorService = createGeneratorService()
  ): GeneratorWithService = {
    val form = createGeneratorForm(service = service)

    generatorsDao.upsert(testUser, form)
    generatorsDao.findAll(
      Authorization.All,
      serviceGuid = Some(service.guid),
      key = Some(form.generator.key),
      limit = 1
    ).headOption.getOrElse {
      sys.error("Failed to create generator")
    }
  }

  def createGeneratorForm(
    service: GeneratorService = createGeneratorService(),
    attributes: Seq[String] = Nil
  ): GeneratorForm = {
    val value = UUID.randomUUID.toString.toLowerCase
    GeneratorForm(
      serviceGuid = service.guid,
      generator = Generator(
        key = "test_" + value,
        name = "Test " + value,
        description = None,
        language = None,
        attributes = attributes
      )
    )
  }

}
