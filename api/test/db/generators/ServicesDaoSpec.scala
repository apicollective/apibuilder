package db.generators

import java.util.UUID

import db.Authorization
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class ServicesDaoSpec extends PlaySpec with GuiceOneAppPerSuite with GeneratorHelpers {

  "validate" in {
    val form = createGeneratorServiceForm()
    servicesDao.validate(form) must be(Nil)
    servicesDao.validate(form.copy(uri = "foo")).map(_.message) must be(Seq("URI[foo] must start with http://, https://, or file://"))

    servicesDao.create(testUser, form)
    servicesDao.validate(form).map(_.message) must be(Seq(s"URI[${form.uri}] already exists"))
  }

  "create" must {

    "creates a service" in {
      val form = createGeneratorServiceForm()
      val service = servicesDao.create(testUser, form)
      service.uri must be(form.uri)
    }

    "raises error on duplicate uri" in {
      val form = createGeneratorServiceForm()
      servicesDao.create(testUser, form)
      intercept[AssertionError] {
        servicesDao.create(testUser, form)
      }
    }

  }

  "softDelete" in {
    val service = servicesDao.create(testUser, createGeneratorServiceForm())
    servicesDao.softDelete(testUser, service)
    servicesDao.findByGuid(Authorization.All, service.guid) must be(None)
    servicesDao.findAll(Authorization.All, guid = Some(service.guid), isDeleted = None).map(_.guid) must be(Seq(service.guid))
    servicesDao.findAll(Authorization.All, guid = Some(service.guid), isDeleted = Some(true)).map(_.guid) must be(Seq(service.guid))
  }

  "findAll" must {

    "uri" in {
      val form = createGeneratorServiceForm()
      servicesDao.create(testUser, form)
      servicesDao.findAll(Authorization.All, uri = Some(form.uri)).map(_.uri) must be(Seq(form.uri))
      servicesDao.findAll(Authorization.All, uri = Some(form.uri + "2")) must be(Nil)
    }

    "generatorKey" in {
      val form = createGeneratorServiceForm()
      val service = servicesDao.create(testUser, form)
      val gws = createGenerator(service)

      servicesDao.findAll(Authorization.All, generatorKey = Some(gws.generator.key)).map(_.guid) must be(Seq(service.guid))
      servicesDao.findAll(Authorization.All, guid = Some(service.guid), generatorKey = Some(gws.generator.key)).map(_.guid) must be(Seq(service.guid))
      servicesDao.findAll(Authorization.All, guid = Some(UUID.randomUUID), generatorKey = Some(gws.generator.key)).map(_.guid) must be(Nil)
      servicesDao.findAll(Authorization.All, generatorKey = Some(gws.generator.key + "2")) must be(Nil)
    }

  }

}
