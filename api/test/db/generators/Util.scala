package db.generators

import java.util.UUID

object Util {

  def createGeneratorService(
    form: ServiceForm = createGeneratorServiceForm()
  ): Service = {
    ServicesDao.create(db.Util.createdBy, form)
  }

  def createGeneratorServiceForm(
    uri: String = s"http://test.generator.${UUID.randomUUID}"
  ): ServiceForm = {
    ServiceForm(
      uri = uri
    )
  }

  def createGeneratorRefresh(
    service: Service = createGeneratorService()
  ): Refresh = {
    RefreshesDao.upsert(db.Util.createdBy, service)
    RefreshesDao.findAll(serviceGuid = Some(service.guid)).headOption.getOrElse {
      sys.error("Failed to create refresh")
    }
  }

  def createGenerator(
    service: Service = createGeneratorService(),
    form: GeneratorForm = createGeneratorForm()
  ): Generator = {
    GeneratorsDao.upsert(db.Util.createdBy, service, form)
    GeneratorsDao.findAll(
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
