package db.generators

import db.Authorization
import com.bryzek.apidoc.api.v0.models.{GeneratorForm, GeneratorService, GeneratorServiceForm, GeneratorWithService}
import com.bryzek.apidoc.generator.v0.models.Generator
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
  ): GeneratorWithService = {
    val form = createGeneratorForm(service = service)

    GeneratorsDao.upsert(db.Util.createdBy, form)
    GeneratorsDao.findAll(
      Authorization.All,
      serviceGuid = Some(service.guid),
      key = Some(form.generator.key),
      limit = 1
    ).headOption.getOrElse {
      sys.error("Failed to create generator")
    }
  }

  def createGeneratorForm(
    service: GeneratorService = createGeneratorService()
  ): GeneratorForm = {
    val value = UUID.randomUUID.toString.toLowerCase
    GeneratorForm(
      serviceGuid = service.guid,
      generator = Generator(
        key = "test_" + value,
        name = "Test " + value,
        description = None,
        language = None
      )
    )
  }

}
