package db.generators

import java.util.UUID

import db.Authorization
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class GeneratorsDaoSpec extends PlaySpec with GuiceOneAppPerSuite with GeneratorHelpers {

  "upsert" must {

    "is a no-op if no data has changed" in {
      val service = createGeneratorService()
      val form = createGeneratorForm(service)
      generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)) must be(Nil)

      generatorsDao.upsert(testUser, form)
      val generator = generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      generator.key must be(form.generator.key)

      generatorsDao.upsert(testUser, form)
      val second = generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      second must be(generator)
    }

    "change record if no data has changed" in {
      val service = createGeneratorService()
      val form = createGeneratorForm(service = service)
      generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)) must be(Nil)

      generatorsDao.upsert(testUser, form)
      val generator = generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      generator.key must be(form.generator.key)

      val newForm = form.copy(
        serviceGuid = service.guid,
        generator = form.generator.copy(name = form.generator.name + "2")
      )
      generatorsDao.upsert(testUser, newForm)
      val second = generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      second.name must be(form.generator.name + "2")
      second.key must be(generator.key)
    }

    "stores attributes" in {
      val service = createGeneratorService()
      val form = createGeneratorForm(
        service,
        makeGenerator(
          attributes = Seq("foo", "bar")
        )
      )

      generatorsDao.upsert(testUser, form)
      generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.get.attributes must be(
        Seq("foo", "bar")
      )

      val form2 = form.copy(
        serviceGuid = service.guid,
        generator = form.generator.copy(
          attributes = Seq("baz")
        )
      )
      generatorsDao.upsert(testUser, form2)
      generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.get.attributes must be(
        Seq("baz")
      )

      val form3 = form.copy(
        serviceGuid = service.guid,
        generator = form.generator.copy(
          attributes = Nil
        )
      )
      generatorsDao.upsert(testUser, form3)
      generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).headOption.get.attributes must be(
        Nil
      )
    }

  }

  "softDelete" in {
    val generator = createGenerator()
    generatorsDao.softDelete(testUser, generator)
    generatorsDao.findAll(Authorization.All, key = Some(generator.key)) must be(Nil)
  }

  "findAll" must {

    "serviceGuid" in {
      val service = createGeneratorService()
      val generator = createGenerator(service)
      generatorsDao.findAll(Authorization.All, serviceGuid = Some(service.guid)).map(_.key) must be(Seq(generator.key))
      generatorsDao.findAll(Authorization.All, serviceGuid = Some(UUID.randomUUID)) must be(Nil)
    }

    "isDeleted" in {
      val generator = createGenerator()
      generatorsDao.findAll(Authorization.All, key = Some(generator.key)).map(_.key) must be(Seq(generator.key))

      generatorsDao.softDelete(testUser, generator)
      generatorsDao.findAll(Authorization.All, key = Some(generator.key), isDeleted = None).map(_.key) must be(Seq(generator.key))
      generatorsDao.findAll(Authorization.All, key = Some(generator.key), isDeleted = Some(true)).map(_.key) must be(Seq(generator.key))
      generatorsDao.findAll(Authorization.All, key = Some(generator.key), isDeleted = Some(false)) must be(Nil)
    }

  }

}
