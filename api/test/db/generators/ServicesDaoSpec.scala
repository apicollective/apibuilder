package db.generators

import db.Authorization
import com.gilt.apidoc.api.v0.models.{GeneratorServiceForm}
import org.scalatest.{FunSpec, Matchers}
import java.util.UUID

class ServicesDaoSpec extends FunSpec with Matchers {

  new play.core.StaticApplication(new java.io.File("."))

  it("validate") {
    val form = Util.createGeneratorServiceForm()
    ServicesDao.validate(form) should be(Nil)
    ServicesDao.validate(form.copy(uri = "foo")).map(_.message) should be(Seq("URI[foo] must start with http://, https://, or file://"))

    val service = ServicesDao.create(db.Util.createdBy, form)
    ServicesDao.validate(form).map(_.message) should be(Seq(s"URI[${form.uri}] already exists"))
  }

  describe("create") {

    it("creates a service") {
      val form = Util.createGeneratorServiceForm()
      val service = ServicesDao.create(db.Util.createdBy, form)
      service.uri should be(form.uri)
    }

    it("raises error on duplicate uri") {
      val form = Util.createGeneratorServiceForm()
      ServicesDao.create(db.Util.createdBy, form)
      intercept[AssertionError] {
        ServicesDao.create(db.Util.createdBy, form)
      }
    }

  }

  it("softDelete") {
    val service = ServicesDao.create(db.Util.createdBy, Util.createGeneratorServiceForm())
    ServicesDao.softDelete(db.Util.createdBy, service)
    ServicesDao.findByGuid(Authorization.All, service.guid) should be(None)
    ServicesDao.findAll(Authorization.All, guid = Some(service.guid), isDeleted = None).map(_.guid) should be(Seq(service.guid))
    ServicesDao.findAll(Authorization.All, guid = Some(service.guid), isDeleted = Some(true)).map(_.guid) should be(Seq(service.guid))
  }

  describe("findAll") {

    it("uri") {
      val form = Util.createGeneratorServiceForm()
      val service = ServicesDao.create(db.Util.createdBy, form)
      ServicesDao.findAll(Authorization.All, uri = Some(form.uri)).map(_.uri) should be(Seq(form.uri))
      ServicesDao.findAll(Authorization.All, uri = Some(form.uri + "2")) should be(Nil)
    }

    it("generatorKey") {
      val form = Util.createGeneratorServiceForm()
      val service = ServicesDao.create(db.Util.createdBy, form)
      val generator = Util.createGenerator(service)

      ServicesDao.findAll(Authorization.All, generatorKey = Some(generator.key)).map(_.guid) should be(Seq(service.guid))
      ServicesDao.findAll(Authorization.All, generatorKey = Some(generator.key + "2")) should be(Nil)
    }

  }

}
