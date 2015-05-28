package db.generators

import db.Authorization
import org.scalatest.{FunSpec, Matchers}
import java.util.UUID

class GeneratorsDaoSpec extends FunSpec with Matchers {

  new play.core.StaticApplication(new java.io.File("."))

  describe("upsert") {

    it("is a no-op if no data has changed") {
      val service = Util.createGeneratorService()
      val form = Util.createGeneratorForm()
      GeneratorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)) should be(Nil)

      GeneratorsDao.upsert(db.Util.createdBy, service, form)
      val generator = GeneratorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      generator.service.guid should be(service.guid)
      generator.key should be(form.key)

      GeneratorsDao.upsert(db.Util.createdBy, service, form)
      val second = GeneratorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      second.guid should be(generator.guid)
    }

    it("change record if no data has changed") {
      val service = Util.createGeneratorService()
      val form = Util.createGeneratorForm()
      GeneratorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)) should be(Nil)

      GeneratorsDao.upsert(db.Util.createdBy, service, form)
      val generator = GeneratorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      generator.service.guid should be(service.guid)
      generator.key should be(form.key)

      val newForm = form.copy(name = form.name + "2")
      GeneratorsDao.upsert(db.Util.createdBy, service, newForm)
      val second = GeneratorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      second.guid should not be(generator.guid)
      second.service.guid should be(service.guid)
      second.key should be(form.key)
      second.name should be(newForm.name)
    }

  }

  it("softDelete") {
    val generator = Util.createGenerator()
    GeneratorsDao.softDelete(db.Util.createdBy, generator)
    GeneratorsDao.findAll(Authorization.All, guid = Some(generator.guid)) should be(Nil)
  }

  describe("findAll") {

    it("serviceGuid") {
      val generator = Util.createGenerator()
      GeneratorsDao.findAll(Authorization.All, serviceGuid = Some(generator.service.guid)).map(_.guid) should be(Seq(generator.guid))
      GeneratorsDao.findAll(Authorization.All, serviceGuid = Some(UUID.randomUUID)).map(_.guid) should be(Nil)
    }

    it("isDeleted") {
      val generator = Util.createGenerator()
      GeneratorsDao.findAll(Authorization.All, guid = Some(generator.guid)).map(_.guid) should be(Seq(generator.guid))

      GeneratorsDao.softDelete(db.Util.createdBy, generator)
      GeneratorsDao.findAll(Authorization.All, guid = Some(generator.guid), isDeleted = None).map(_.guid) should be(Seq(generator.guid))
      GeneratorsDao.findAll(Authorization.All, guid = Some(generator.guid), isDeleted = Some(true)).map(_.guid) should be(Seq(generator.guid))
      GeneratorsDao.findAll(Authorization.All, guid = Some(generator.guid), isDeleted = Some(false)) should be(Nil)
    }

  }

}
