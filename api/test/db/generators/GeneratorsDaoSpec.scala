package db.generators

import db.Authorization
import org.scalatest.{FunSpec, Matchers}
import java.util.UUID

class GeneratorsDaoSpec extends FunSpec with Matchers with util.TestApplication {

  describe("upsert") {

    it("is a no-op if no data has changed") {
      val service = Util.createGeneratorService()
      val form = Util.createGeneratorForm(service)
      generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)) should be(Nil)

      generatorsDao.upsert(db.Util.createdBy, form)
      val gws = generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      gws.generator.key should be(form.generator.key)

      generatorsDao.upsert(db.Util.createdBy, form)
      val second = generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      second should be(gws)
    }

    it("change record if no data has changed") {
      val service = Util.createGeneratorService()
      val form = Util.createGeneratorForm(service = service)
      generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)) should be(Nil)

      generatorsDao.upsert(db.Util.createdBy, form)
      val gws = generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      gws.generator.key should be(form.generator.key)

      val newForm = form.copy(
        serviceGuid = service.guid,
        generator = form.generator.copy(name = form.generator.name + "2")
      )
      generatorsDao.upsert(db.Util.createdBy, newForm)
      val second = generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      second.generator.name should be(form.generator.name + "2")
      second.generator.key should be(gws.generator.key)
    }

    it("stores attributes") {
      val service = Util.createGeneratorService()
      val form = Util.createGeneratorForm(
        service,
        attributes = Seq("foo", "bar")
      )

      generatorsDao.upsert(db.Util.createdBy, form)
      generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.get.generator.attributes should be(
        Seq("foo", "bar")
      )

      val form2 = form.copy(
        serviceGuid = service.guid,
        generator = form.generator.copy(
          attributes = Seq("baz")
        )
      )
      generatorsDao.upsert(db.Util.createdBy, form2)
      generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.get.generator.attributes should be(
        Seq("baz")
      )

      val form3 = form.copy(
        serviceGuid = service.guid,
        generator = form.generator.copy(
          attributes = Nil
        )
      )
      generatorsDao.upsert(db.Util.createdBy, form3)
      generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.get.generator.attributes should be(
        Nil
      )
    }

  }

  it("softDelete") {
    val gws = Util.createGenerator()
    generatorsDao.softDelete(db.Util.createdBy, gws)
    generatorsDao.findAll(Authorization.All, key = Some(gws.generator.key)) should be(Nil)
  }

  describe("findAll") {

    it("serviceGuid") {
      val service = Util.createGeneratorService()
      val gws = Util.createGenerator(service)
      generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).map(_.generator.key) should be(Seq(gws.generator.key))
      generatorsDao.findAll(Authorization.All, serviceGuid = Some(UUID.randomUUID)) should be(Nil)
    }

    it("isDeleted") {
      val gws = Util.createGenerator()
      generatorsDao.findAll(Authorization.All, key = Some(gws.generator.key)).map(_.generator.key) should be(Seq(gws.generator.key))

      generatorsDao.softDelete(db.Util.createdBy, gws)
      generatorsDao.findAll(Authorization.All, key = Some(gws.generator.key), isDeleted = None).map(_.generator.key) should be(Seq(gws.generator.key))
      generatorsDao.findAll(Authorization.All, key = Some(gws.generator.key), isDeleted = Some(true)).map(_.generator.key) should be(Seq(gws.generator.key))
      generatorsDao.findAll(Authorization.All, key = Some(gws.generator.key), isDeleted = Some(false)) should be(Nil)
    }

  }

}
