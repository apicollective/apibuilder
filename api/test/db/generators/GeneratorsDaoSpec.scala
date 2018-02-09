package db.generators

import db.Authorization
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import java.util.UUID

class GeneratorsDaoSpec extends PlaySpec with OneAppPerSuite with util.Daos {

  describe("upsert") {

    it("is a no-op if no data has changed") {
      val service = Util.createGeneratorService()
      val form = Util.createGeneratorForm(service)
      generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)) must be(Nil)

      generatorsDao.upsert(db.Util.createdBy, form)
      val gws = generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      gws.generator.key must be(form.generator.key)

      generatorsDao.upsert(db.Util.createdBy, form)
      val second = generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      second must be(gws)
    }

    it("change record if no data has changed") {
      val service = Util.createGeneratorService()
      val form = Util.createGeneratorForm(service = service)
      generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)) must be(Nil)

      generatorsDao.upsert(db.Util.createdBy, form)
      val gws = generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      gws.generator.key must be(form.generator.key)

      val newForm = form.copy(
        serviceGuid = service.guid,
        generator = form.generator.copy(name = form.generator.name + "2")
      )
      generatorsDao.upsert(db.Util.createdBy, newForm)
      val second = generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      second.generator.name must be(form.generator.name + "2")
      second.generator.key must be(gws.generator.key)
    }

    it("stores attributes") {
      val service = Util.createGeneratorService()
      val form = Util.createGeneratorForm(
        service,
        attributes = Seq("foo", "bar")
      )

      generatorsDao.upsert(db.Util.createdBy, form)
      generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.get.generator.attributes must be(
        Seq("foo", "bar")
      )

      val form2 = form.copy(
        serviceGuid = service.guid,
        generator = form.generator.copy(
          attributes = Seq("baz")
        )
      )
      generatorsDao.upsert(db.Util.createdBy, form2)
      generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.get.generator.attributes must be(
        Seq("baz")
      )

      val form3 = form.copy(
        serviceGuid = service.guid,
        generator = form.generator.copy(
          attributes = Nil
        )
      )
      generatorsDao.upsert(db.Util.createdBy, form3)
      generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.get.generator.attributes must be(
        Nil
      )
    }

  }

  it("softDelete") {
    val gws = Util.createGenerator()
    generatorsDao.softDelete(db.Util.createdBy, gws)
    generatorsDao.findAll(Authorization.All, key = Some(gws.generator.key)) must be(Nil)
  }

  describe("findAll") {

    it("serviceGuid") {
      val service = Util.createGeneratorService()
      val gws = Util.createGenerator(service)
      generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).map(_.generator.key) must be(Seq(gws.generator.key))
      generatorsDao.findAll(Authorization.All, serviceGuid = Some(UUID.randomUUID)) must be(Nil)
    }

    it("isDeleted") {
      val gws = Util.createGenerator()
      generatorsDao.findAll(Authorization.All, key = Some(gws.generator.key)).map(_.generator.key) must be(Seq(gws.generator.key))

      generatorsDao.softDelete(db.Util.createdBy, gws)
      generatorsDao.findAll(Authorization.All, key = Some(gws.generator.key), isDeleted = None).map(_.generator.key) must be(Seq(gws.generator.key))
      generatorsDao.findAll(Authorization.All, key = Some(gws.generator.key), isDeleted = Some(true)).map(_.generator.key) must be(Seq(gws.generator.key))
      generatorsDao.findAll(Authorization.All, key = Some(gws.generator.key), isDeleted = Some(false)) must be(Nil)
    }

  }

}
