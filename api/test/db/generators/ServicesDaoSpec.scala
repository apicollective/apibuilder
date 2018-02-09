package db.generators

import db.Authorization
import io.apibuilder.api.v0.models.{GeneratorServiceForm}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import java.util.UUID

class ServicesDaoSpec extends PlaySpec with OneAppPerSuite with util.Daos {

  it("validate") {
    val form = Util.createGeneratorServiceForm()
    servicesDao.validate(form) must be(Nil)
    servicesDao.validate(form.copy(uri = "foo")).map(_.message) must be(Seq("URI[foo] must start with http://, https://, or file://"))

    val service = servicesDao.create(db.Util.createdBy, form)
    servicesDao.validate(form).map(_.message) must be(Seq(s"URI[${form.uri}] already exists"))
  }

  describe("create") {

    it("creates a service") {
      val form = Util.createGeneratorServiceForm()
      val service = servicesDao.create(db.Util.createdBy, form)
      service.uri must be(form.uri)
    }

    it("raises error on duplicate uri") {
      val form = Util.createGeneratorServiceForm()
      servicesDao.create(db.Util.createdBy, form)
      intercept[AssertionError] {
        servicesDao.create(db.Util.createdBy, form)
      }
    }

  }

  it("softDelete") {
    val service = servicesDao.create(db.Util.createdBy, Util.createGeneratorServiceForm())
    servicesDao.softDelete(db.Util.createdBy, service)
    servicesDao.findByGuid(Authorization.All, service.guid) must be(None)
    servicesDao.findAll(Authorization.All, guid = Some(service.guid), isDeleted = None).map(_.guid) must be(Seq(service.guid))
    servicesDao.findAll(Authorization.All, guid = Some(service.guid), isDeleted = Some(true)).map(_.guid) must be(Seq(service.guid))
  }

  describe("findAll") {

    it("uri") {
      val form = Util.createGeneratorServiceForm()
      val service = servicesDao.create(db.Util.createdBy, form)
      servicesDao.findAll(Authorization.All, uri = Some(form.uri)).map(_.uri) must be(Seq(form.uri))
      servicesDao.findAll(Authorization.All, uri = Some(form.uri + "2")) must be(Nil)
    }

    it("generatorKey") {
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
