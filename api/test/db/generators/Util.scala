package db.generators

import db.Authorization
import com.gilt.apidoc.api.v0.models.{Generator, GeneratorService, GeneratorServiceForm, Visibility}
import java.util.UUID

object Util {

  def createGeneratorService(
    form: GeneratorServiceForm = createGeneratorServiceForm()
  ): GeneratorService = {
    ServicesDao.create(db.Util.createdBy, form)
  }

  def createGeneratorServiceForm(
    uri: String = s"http://test.generator.${UUID.randomUUID}",
    visibility: Visibility = Visibility.Public
  ): GeneratorServiceForm = {
    GeneratorServiceForm(
      uri = uri,
      visibility = visibility
    )
  }

  def createGeneratorRefresh(
    service: GeneratorService = createGeneratorService()
  ): Refresh = {
    RefreshesDao.upsert(db.Util.createdBy, service)
    RefreshesDao.findAll(serviceGuid = Some(service.guid)).headOption.getOrElse {
      sys.error("Failed to create refresh")
    }
  }

  def createGenerator(
    service: GeneratorService = createGeneratorService(),
    form: GeneratorForm = createGeneratorForm()
  ): Generator = {
    GeneratorsDao.upsert(db.Util.createdBy, service, form)
    GeneratorsDao.findAll(
      Authorization.All,
      serviceGuid = Some(service.guid),
      key = Some(form.key),
      limit = 1
    ).headOption.getOrElse {
      sys.error("Failed to create generator")
    }
  }

  def createGeneratorForm(): GeneratorForm = {
    val value = UUID.randomUUID.toString.toLowerCase
    GeneratorForm(
      key = "test_" + value,
      name = "Test " + value,
      description = None,
      language = None
    )
  }
}
