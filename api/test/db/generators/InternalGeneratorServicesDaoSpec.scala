package db.generators

import helpers.ValidatedTestHelpers
import io.apibuilder.api.v0.models.GeneratorServiceForm
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import java.util.UUID

class InternalGeneratorServicesDaoSpec extends PlaySpec with GuiceOneAppPerSuite with GeneratorHelpers with ValidatedTestHelpers {

  "validate" in {
    def setupInvalid(form: GeneratorServiceForm): Seq[String] = {
      expectInvalid(
        servicesDao.validate(form)
      ).map(_.message)
    }
    val form = makeGeneratorServiceForm()
    expectValid {
      servicesDao.validate(form)
    }
    setupInvalid(form.copy(uri = "foo")) must be(Seq("URI[foo] must start with http://, https://, or file://"))

    servicesDao.create(testUser, form)
    setupInvalid(form) must be(Seq(s"URI[${form.uri}] already exists"))
  }

  "create" must {

    "creates a service" in {
      val form = makeGeneratorServiceForm()
      val service = createGeneratorService(form)
      service.uri must be(form.uri)
    }

    "raises error on duplicate uri" in {
      val form = makeGeneratorServiceForm()
      createGeneratorService(form)

      expectInvalid {
        servicesDao.create(testUser, form)
      }.head.message.contains("already exists") mustBe true
    }

  }

  "softDelete" in {
    val service = createGeneratorService()
    servicesDao.softDelete(testUser, service)
    servicesDao.findByGuid(service.guid) must be(None)
    servicesDao.findAll(guid = Some(service.guid), isDeleted = None, limit = None).map(_.guid) must be(Seq(service.guid))
    servicesDao.findAll(guid = Some(service.guid), isDeleted = Some(true), limit = None).map(_.guid) must be(Seq(service.guid))
  }

  "findAll" must {

    "uri" in {
      val form = makeGeneratorServiceForm()
      servicesDao.create(testUser, form)
      servicesDao.findAll(uri = Some(form.uri), limit = None).map(_.uri) must be(Seq(form.uri))
      servicesDao.findAll(uri = Some(form.uri + "2"), limit = None) must be(Nil)
    }

    "generatorKey" in {
      val service = createGeneratorService()
      val generator = createGenerator(service)

      servicesDao.findAll(generatorKey = Some(generator.key), limit = None).map(_.guid) must be(Seq(service.guid))
      servicesDao.findAll(guid = Some(service.guid), generatorKey = Some(generator.key), limit = None).map(_.guid) must be(Seq(service.guid))
      servicesDao.findAll(guid = Some(UUID.randomUUID), generatorKey = Some(generator.key), limit = None).map(_.guid) must be(Nil)
      servicesDao.findAll(generatorKey = Some(generator.key + "2"), limit = None) must be(Nil)
    }

  }

}
