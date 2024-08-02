package db.generators

import db.Authorization
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import java.util.UUID

class GeneratorsDaoSpec extends PlaySpec with GuiceOneAppPerSuite with GeneratorHelpers {

  "upsert" must {

    "is a no-op if no data has changed" in {
      val service = createGeneratorService()
      val form = createGeneratorForm(service)
      generatorsDao.findAll(serviceGuid = Some(service.guid), limit = None) must be(Nil)

      val generator = expectValid {
        generatorsDao.upsert(testUser, form)
      }
      generator.key must be(form.generator.key)

      generatorsDao.upsert(testUser, form)
      val second = generatorsDao.findAll(serviceGuid = Some(service.guid), limit = None).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      second must be(generator)
    }

    "change record if no data has changed" in {
      val service = createGeneratorService()
      val form = createGeneratorForm(service = service)
      generatorsDao.findAll(serviceGuid = Some(service.guid), limit = None) must be(Nil)

      generatorsDao.upsert(testUser, form)
      val generator = generatorsDao.findAll(serviceGuid = Some(service.guid), limit = None).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      generator.key must be(form.generator.key)

      val newForm = form.copy(
        serviceGuid = service.guid,
        generator = form.generator.copy(name = form.generator.name + "2")
      )
      generatorsDao.upsert(testUser, newForm)
      val second = generatorsDao.findAll(serviceGuid = Some(service.guid), limit = None).headOption.getOrElse {
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
      generatorsDao.findAll(serviceGuid = Some(service.guid), limit = None).headOption.get.attributes must be(
        Seq("foo", "bar")
      )

      val form2 = form.copy(
        serviceGuid = service.guid,
        generator = form.generator.copy(
          attributes = Seq("baz")
        )
      )
      generatorsDao.upsert(testUser, form2)
      generatorsDao.findAll(serviceGuid = Some(service.guid), limit = None).headOption.get.attributes must be(
        Seq("baz")
      )

      val form3 = form.copy(
        serviceGuid = service.guid,
        generator = form.generator.copy(
          attributes = Nil
        )
      )
      generatorsDao.upsert(testUser, form3)
      generatorsDao.findAll(serviceGuid = Some(service.guid), limit = None).headOption.get.attributes must be(
        Nil
      )
    }

  }

  "softDelete" in {
    val generator = createGenerator()
    generatorsDao.softDelete(testUser, generator)
    generatorsDao.findAll(key = Some(generator.key), limit = None) must be(Nil)
  }

  "findAll" must {

    "serviceGuid" in {
      val service = createGeneratorService()
      val generator = createGenerator(service)
      generatorsDao.findAll(serviceGuid = Some(service.guid), limit = None).map(_.key) must be(Seq(generator.key))
      generatorsDao.findAll(serviceGuid = Some(UUID.randomUUID), limit = None) must be(Nil)
    }

    "isDeleted" in {
      val generator = createGenerator()
      generatorsDao.findAll(key = Some(generator.key), limit = None).map(_.key) must be(Seq(generator.key))

      generatorsDao.softDelete(testUser, generator)
      generatorsDao.findAll(key = Some(generator.key), isDeleted = None, limit = None).map(_.key) must be(Seq(generator.key))
      generatorsDao.findAll(key = Some(generator.key), isDeleted = Some(true), limit = None).map(_.key) must be(Seq(generator.key))
      generatorsDao.findAll(key = Some(generator.key), isDeleted = Some(false), limit = None) must be(Nil)
    }

  }

}
