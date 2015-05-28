package db.generators

import org.scalatest.{FunSpec, Matchers}
import java.util.UUID

class GeneratorsDaoSpec extends FunSpec with Matchers {

  new play.core.StaticApplication(new java.io.File("."))

  describe("upsert") {

    it("is a no-op if no data has changed") {
      val source = Util.createGeneratorSource()
      val form = Util.createGeneratorForm()
      GeneratorsDao.findAll(sourceGuid = Some(source.guid)) should be(Nil)

      GeneratorsDao.upsert(db.Util.createdBy, source, form)
      val generator = GeneratorsDao.findAll(sourceGuid = Some(source.guid)).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      generator.source.guid should be(source.guid)
      generator.key should be(form.key)

      GeneratorsDao.upsert(db.Util.createdBy, source, form)
      val second = GeneratorsDao.findAll(sourceGuid = Some(source.guid)).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      second.guid should be(generator.guid)
    }

    it("change record if no data has changed") {
      val source = Util.createGeneratorSource()
      val form = Util.createGeneratorForm()
      GeneratorsDao.findAll(sourceGuid = Some(source.guid)) should be(Nil)

      GeneratorsDao.upsert(db.Util.createdBy, source, form)
      val generator = GeneratorsDao.findAll(sourceGuid = Some(source.guid)).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      generator.source.guid should be(source.guid)
      generator.key should be(form.key)

      val newForm = form.copy(name = form.name + "2")
      GeneratorsDao.upsert(db.Util.createdBy, source, newForm)
      val second = GeneratorsDao.findAll(sourceGuid = Some(source.guid)).headOption.getOrElse {
        sys.error("Failed to create generator record")
      }
      second.guid should not be(generator.guid)
      second.source.guid should be(source.guid)
      second.key should be(form.key)
      second.name should be(newForm.name)
    }

  }

  it("softDelete") {
    val generator = Util.createGenerator()
    GeneratorsDao.softDelete(db.Util.createdBy, generator)
    GeneratorsDao.findAll(guid = Some(generator.guid)) should be(Nil)
  }

  describe("findAll") {

    it("sourceGuid") {
      val generator = Util.createGenerator()
      GeneratorsDao.findAll(sourceGuid = Some(generator.source.guid)).map(_.guid) should be(Seq(generator.guid))
      GeneratorsDao.findAll(sourceGuid = Some(UUID.randomUUID)).map(_.guid) should be(Nil)
    }

    it("isDeleted") {
      val generator = Util.createGenerator()
      GeneratorsDao.findAll(guid = Some(generator.guid)).map(_.guid) should be(Seq(generator.guid))

      GeneratorsDao.softDelete(db.Util.createdBy, generator)
      GeneratorsDao.findAll(guid = Some(generator.guid), isDeleted = None).map(_.guid) should be(Seq(generator.guid))
      GeneratorsDao.findAll(guid = Some(generator.guid), isDeleted = Some(true)).map(_.guid) should be(Seq(generator.guid))
      GeneratorsDao.findAll(guid = Some(generator.guid), isDeleted = Some(false)) should be(Nil)
    }

  }

}
