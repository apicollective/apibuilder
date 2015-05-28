package db.generators

import db.Authorization
import com.gilt.apidoc.api.v0.models.{GeneratorServiceForm, Visibility}
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

    it("authorization") {
      val u1 = db.Util.createRandomUser()
      val u2 = db.Util.createRandomUser()
      val u3 = db.Util.createRandomUser()

      val public = ServicesDao.create(u1, Util.createGeneratorServiceForm(visibility = Visibility.Public))
      val user = ServicesDao.create(u2, Util.createGeneratorServiceForm(visibility = Visibility.User))
      val org = ServicesDao.create(u3, Util.createGeneratorServiceForm(visibility = Visibility.Organization))

      ServicesDao.findAll(Authorization.All, uri = Some(public.uri)).map(_.guid) should be(Seq(public.guid))
      ServicesDao.findAll(Authorization.All, uri = Some(user.uri)).map(_.guid) should be(Seq(user.guid))
      ServicesDao.findAll(Authorization.All, uri = Some(org.uri)).map(_.guid) should be(Seq(org.guid))

      val u1Auth = Authorization.User(u1.guid)
      val u2Auth = Authorization.User(u2.guid)
      val u3Auth = Authorization.User(u3.guid)

      ServicesDao.findAll(u1Auth, uri = Some(public.uri)).map(_.guid) should be(Seq(public.guid))
      ServicesDao.findAll(u1Auth, uri = Some(user.uri)).map(_.guid) should be(Nil)
      ServicesDao.findAll(u1Auth, uri = Some(org.uri)).map(_.guid) should be(Nil)

      ServicesDao.findAll(u2Auth, uri = Some(public.uri)).map(_.guid) should be(Seq(public.guid))
      ServicesDao.findAll(u2Auth, uri = Some(user.uri)).map(_.guid) should be(Seq(user.guid))
      ServicesDao.findAll(u2Auth, uri = Some(org.uri)).map(_.guid) should be(Nil)

      // ServicesDao.findAll(u1Auth, uri = Some(public.uri)).map(_.guid) should be(Seq(public.guid))
      // ServicesDao.findAll(u1Auth, uri = Some(user.uri)).map(_.guid) should be(Seq(user.guid))
      // ServicesDao.findAll(u1Auth, uri = Some(org.uri)).map(_.guid) should be(Nil)
    }

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
