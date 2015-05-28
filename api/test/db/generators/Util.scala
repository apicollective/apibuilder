package db.generators

import db.Authorization
import com.gilt.apidoc.api.v0.models.{GeneratorService, GeneratorServiceForm}
import com.gilt.apidoc.generator.v0.models.Generator
import java.util.UUID

object Util {

  def createGeneratorService(
    form: GeneratorServiceForm = createGeneratorServiceForm()
  ): GeneratorService = {
    ServicesDao.create(db.Util.createdBy, form)
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
  ): Generator = {
    val gen = createGeneratorForm()

    GeneratorsDao.upsert(db.Util.createdBy, service, gen)
    GeneratorsDao.findAll(
      Authorization.All,
      serviceGuid = Some(service.guid),
      key = Some(gen.key),
      limit = 1
    ).headOption.getOrElse {
      sys.error("Failed to create generator")
    }
  }

  def createGeneratorForm(): Generator = {
    val value = UUID.randomUUID.toString.toLowerCase
    Generator(
      key = "test_" + value,
      name = "Test " + value,
      description = None,
      language = None
    )
  }
}
