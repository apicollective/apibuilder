package db.generators

import db.Authorization
import com.bryzek.apidoc.api.v0.models.{GeneratorServiceForm}
import org.scalatest.{FunSpec, Matchers}
import java.util.UUID

class ServicesDaoSpec extends FunSpec with Matchers with util.TestApplication {

  it("validate") {
    val form = Util.createGeneratorServiceForm()
    servicesDao.validate(form) should be(Nil)
    servicesDao.validate(form.copy(uri = "foo")).map(_.message) should be(Seq("URI[foo] must start with http://, https://, or file://"))

    val service = servicesDao.create(db.Util.createdBy, form)
    servicesDao.validate(form).map(_.message) should be(Seq(s"URI[${form.uri}] already exists"))
  }

  describe("create") {

    it("creates a service") {
      val form = Util.createGeneratorServiceForm()
      val service = servicesDao.create(db.Util.createdBy, form)
      service.uri should be(form.uri)
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
    servicesDao.findByGuid(Authorization.All, service.guid) should be(None)
    servicesDao.findAll(Authorization.All, guid = Some(service.guid), isDeleted = None).map(_.guid) should be(Seq(service.guid))
    servicesDao.findAll(Authorization.All, guid = Some(service.guid), isDeleted = Some(true)).map(_.guid) should be(Seq(service.guid))
  }

  describe("findAll") {

    it("uri") {
      val form = Util.createGeneratorServiceForm()
      val service = servicesDao.create(db.Util.createdBy, form)
      servicesDao.findAll(Authorization.All, uri = Some(form.uri)).map(_.uri) should be(Seq(form.uri))
      servicesDao.findAll(Authorization.All, uri = Some(form.uri + "2")) should be(Nil)
    }

    it("generatorKey") {
      val form = Util.createGeneratorServiceForm()
      val service = servicesDao.create(db.Util.createdBy, form)
      val gws = Util.createGenerator(service)

      servicesDao.findAll(Authorization.All, generatorKey = Some(gws.generator.key)).map(_.guid) should be(Seq(service.guid))
      servicesDao.findAll(Authorization.All, guid = Some(service.guid), generatorKey = Some(gws.generator.key)).map(_.guid) should be(Seq(service.guid))
      servicesDao.findAll(Authorization.All, guid = Some(UUID.randomUUID), generatorKey = Some(gws.generator.key)).map(_.guid) should be(Nil)
      servicesDao.findAll(Authorization.All, generatorKey = Some(gws.generator.key + "2")) should be(Nil)
    }

  }

}
