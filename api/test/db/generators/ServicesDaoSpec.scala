package db.generators

import db.Authorization
import io.apibuilder.api.v0.models.{GeneratorServiceForm}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import java.util.UUID

class ServicesDaoSpec extends PlaySpec with OneAppPerSuite with util.Daos {

  "validate" in {
    val form = Util.createGeneratorServiceForm()
    servicesDao.validate(form) must be(Nil)
    servicesDao.validate(form.copy(uri = "foo")).map(_.message) must be(Seq("URI[foo] must start with http://, https://, or file://"))

    val service = servicesDao.create(db.Util.createdBy, form)
    servicesDao.validate(form).map(_.message) must be(Seq(s"URI[${form.uri}] already exists"))
  }

  "create" must {

    "creates a service" in {
      val form = Util.createGeneratorServiceForm()
      val service = servicesDao.create(db.Util.createdBy, form)
      service.uri must be(form.uri)
    }

    "raises error on duplicate uri" in {
      val form = Util.createGeneratorServiceForm()
      servicesDao.create(db.Util.createdBy, form)
      intercept[AssertionError] {
        servicesDao.create(db.Util.createdBy, form)
      }
    }

  }

  "softDelete" in {
    val service = servicesDao.create(db.Util.createdBy, Util.createGeneratorServiceForm())
    servicesDao.softDelete(db.Util.createdBy, service)
    servicesDao.findByGuid(Authorization.All, service.guid) must be(None)
    servicesDao.findAll(Authorization.All, guid = Some(service.guid), isDeleted = None).map(_.guid) must be(Seq(service.guid))
    servicesDao.findAll(Authorization.All, guid = Some(service.guid), isDeleted = Some(true)).map(_.guid) must be(Seq(service.guid))
  }

  "findAll" must {

    "uri" in {
      val form = Util.createGeneratorServiceForm()
      val service = servicesDao.create(db.Util.createdBy, form)
      servicesDao.findAll(Authorization.All, uri = Some(form.uri)).map(_.uri) must be(Seq(form.uri))
      servicesDao.findAll(Authorization.All, uri = Some(form.uri + "2")) must be(Nil)
    }

    "generatorKey" in {
      val form = Util.createGeneratorServiceForm()
      val service = servicesDao.create(db.Util.createdBy, form)
      val gws = Util.createGenerator(service)

      servicesDao.findAll(Authorization.All, generatorKey = Some(gws.generator.key)).map(_.guid) must be(Seq(service.guid))
      servicesDao.findAll(Authorization.All, guid = Some(service.guid), generatorKey = Some(gws.generator.key)).map(_.guid) must be(Seq(service.guid))
      servicesDao.findAll(Authorization.All, guid = Some(UUID.randomUUID), generatorKey = Some(gws.generator.key)).map(_.guid) must be(Nil)
      servicesDao.findAll(Authorization.All, generatorKey = Some(gws.generator.key + "2")) must be(Nil)
    }

  }

}
