package db.generators

import db.Authorization
import org.scalatest.{FunSpec, Matchers}
import java.util.UUID

class GeneratorsDaoSpec extends FunSpec with Matchers with util.TestApplication {

  describe("upsert") {

    it("is a no-op if no data has changed") {
      val service = Util.createGeneratorService()
      val form = Util.createGeneratorForm()
      GeneratorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)) should be(Nil)

      GeneratorsDao.upsert(db.Util.createdBy, service, form)
      val generator = GeneratorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      generator.key should be(form.key)

      GeneratorsDao.upsert(db.Util.createdBy, service, form)
      val second = GeneratorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      second should be(generator)
    }

    it("change record if no data has changed") {
      val service = Util.createGeneratorService()
      val form = Util.createGeneratorForm()
      GeneratorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)) should be(Nil)

      GeneratorsDao.upsert(db.Util.createdBy, service, form)
      val generator = GeneratorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      generator.key should be(form.key)

      val newForm = form.copy(name = form.name + "2")
      GeneratorsDao.upsert(db.Util.createdBy, service, newForm)
      val second = GeneratorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      second should not be(generator)
    }

  }

  it("softDelete") {
    val generator = Util.createGenerator()
    GeneratorsDao.softDelete(db.Util.createdBy, generator)
    GeneratorsDao.findAll(Authorization.All, key = Some(generator.key)) should be(Nil)
  }

  describe("findAll") {

    it("serviceGuid") {
      val service = Util.createGeneratorService()
      val generator = Util.createGenerator(service)
      GeneratorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).map(_.key) should be(Seq(generator.key))
      GeneratorsDao.findAll(Authorization.All, serviceGuid = Some(UUID.randomUUID)) should be(Nil)
    }

    it("isDeleted") {
      val generator = Util.createGenerator()
      GeneratorsDao.findAll(Authorization.All, key = Some(generator.key)).map(_.key) should be(Seq(generator.key))

      GeneratorsDao.softDelete(db.Util.createdBy, generator)
      GeneratorsDao.findAll(Authorization.All, key = Some(generator.key), isDeleted = None).map(_.key) should be(Seq(generator.key))
      GeneratorsDao.findAll(Authorization.All, key = Some(generator.key), isDeleted = Some(true)).map(_.key) should be(Seq(generator.key))
      GeneratorsDao.findAll(Authorization.All, key = Some(generator.key), isDeleted = Some(false)) should be(Nil)
    }

  }

}
