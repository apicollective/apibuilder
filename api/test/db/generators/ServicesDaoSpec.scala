package db.generators

import org.scalatest.{FunSpec, Matchers}
import java.util.UUID

class ServicesDaoSpec extends FunSpec with Matchers {

  new play.core.StaticApplication(new java.io.File("."))

  it("validate") {
    ServicesDao.validate(ServiceForm("http://foo.com")) should be(Nil)
    ServicesDao.validate(ServiceForm("foo")).map(_.message) should be(Seq("URI[foo] must start with http://, https://, or file://"))

    val form = Util.createGeneratorServiceForm()
    val gen = ServicesDao.create(db.Util.createdBy, form)
    ServicesDao.validate(form).map(_.message) should be(Seq(s"URI[${form.uri}] already exists"))
  }

  describe("create") {

    it("creates a service") {
      val form = Util.createGeneratorServiceForm()
      val gen = ServicesDao.create(db.Util.createdBy, form)
      gen.uri should be(form.uri)
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
    val gen = ServicesDao.create(db.Util.createdBy, Util.createGeneratorServiceForm())
    ServicesDao.softDelete(db.Util.createdBy, gen)
    ServicesDao.findByGuid(gen.guid) should be(None)
    ServicesDao.findAll(guid = Some(gen.guid), isDeleted = None).map(_.guid) should be(Seq(gen.guid))
    ServicesDao.findAll(guid = Some(gen.guid), isDeleted = Some(true)).map(_.guid) should be(Seq(gen.guid))
  }

  describe("findAll") {

    it("uri") {
      val form = Util.createGeneratorServiceForm()
      val gen = ServicesDao.create(db.Util.createdBy, form)
      ServicesDao.findAll(uri = Some(form.uri)).map(_.uri) should be(Seq(form.uri))
      ServicesDao.findAll(uri = Some(form.uri + "2")) should be(Nil)
    }

  }

}
