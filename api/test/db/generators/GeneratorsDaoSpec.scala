package db.generators

import db.Authorization
import org.scalatest.{FunSpec, Matchers}
import java.util.UUID

class GeneratorsDaoSpec extends FunSpec with Matchers with util.TestApplication {

  describe("upsert") {

    it("is a no-op if no data has changed") {
      val service = Util.createGeneratorService()
      val form = Util.createGeneratorForm(service)
      GeneratorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)) should be(Nil)

      GeneratorsDao.upsert(db.Util.createdBy, form)
      val gws = GeneratorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      gws.generator.key should be(form.generator.key)

      GeneratorsDao.upsert(db.Util.createdBy, form)
      val second = GeneratorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      second should be(gws)
    }

    it("change record if no data has changed") {
      val service = Util.createGeneratorService()
      val form = Util.createGeneratorForm(service = service)
      GeneratorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)) should be(Nil)

      GeneratorsDao.upsert(db.Util.createdBy, form)
      val gws = GeneratorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      gws.generator.key should be(form.generator.key)

      val newForm = form.copy(
        serviceGuid = service.guid,
        generator = form.generator.copy(name = form.generator.name + "2")
      )
      GeneratorsDao.upsert(db.Util.createdBy, newForm)
      val second = GeneratorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      second.generator.name should be(form.generator.name + "2")
      second.generator.key should be(gws.generator.key)
    }

  }

  it("softDelete") {
    val gws = Util.createGenerator()
    GeneratorsDao.softDelete(db.Util.createdBy, gws)
    GeneratorsDao.findAll(Authorization.All, key = Some(gws.generator.key)) should be(Nil)
  }

  describe("findAll") {

    it("serviceGuid") {
      val service = Util.createGeneratorService()
      val gws = Util.createGenerator(service)
      GeneratorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).map(_.generator.key) should be(Seq(gws.generator.key))
      GeneratorsDao.findAll(Authorization.All, serviceGuid = Some(UUID.randomUUID)) should be(Nil)
    }

    it("isDeleted") {
      val gws = Util.createGenerator()
      GeneratorsDao.findAll(Authorization.All, key = Some(gws.generator.key)).map(_.generator.key) should be(Seq(gws.generator.key))

      GeneratorsDao.softDelete(db.Util.createdBy, gws)
      GeneratorsDao.findAll(Authorization.All, key = Some(gws.generator.key), isDeleted = None).map(_.generator.key) should be(Seq(gws.generator.key))
      GeneratorsDao.findAll(Authorization.All, key = Some(gws.generator.key), isDeleted = Some(true)).map(_.generator.key) should be(Seq(gws.generator.key))
      GeneratorsDao.findAll(Authorization.All, key = Some(gws.generator.key), isDeleted = Some(false)) should be(Nil)
    }

  }

}
