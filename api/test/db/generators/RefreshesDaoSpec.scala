package db.generators

import org.scalatest.{FunSpec, Matchers}
import java.util.UUID

class RefreshesDaoSpec extends FunSpec with Matchers {

  new play.core.StaticApplication(new java.io.File("."))

  it("upsert") {
    val source = Util.createGeneratorSource()
    RefreshesDao.findAll(sourceGuid = Some(source.guid)) should be(Nil)

    RefreshesDao.upsert(db.Util.createdBy, source)
    val refresh = RefreshesDao.findAll(sourceGuid = Some(source.guid)).headOption.getOrElse {
      sys.error("Failed to create refresh record")
    }

    refresh.source.guid should be(source.guid)

    RefreshesDao.upsert(db.Util.createdBy, source)
    val second = RefreshesDao.findAll(sourceGuid = Some(source.guid)).headOption.getOrElse {
      sys.error("Failed to create refresh record")
    }
    second.guid should be(refresh.guid)
    //second.checkedAt should beGreaterThanOrEqual(refresh.checkedAt)
  }

  it("softDelete") {
    val refresh = Util.createGeneratorRefresh()
    RefreshesDao.softDelete(db.Util.createdBy, refresh)
    RefreshesDao.findAll(guid = Some(refresh.guid)) should be(Nil)
  }

  describe("findAll") {

    it("sourceGuid") {
      val refresh = Util.createGeneratorRefresh()
      RefreshesDao.findAll(sourceGuid = Some(refresh.source.guid)).map(_.guid) should be(Seq(refresh.guid))
      RefreshesDao.findAll(sourceGuid = Some(UUID.randomUUID)).map(_.guid) should be(Nil)
    }

    it("isDeleted") {
      val refresh = Util.createGeneratorRefresh()
      RefreshesDao.findAll(guid = Some(refresh.guid)).map(_.guid) should be(Seq(refresh.guid))

      RefreshesDao.softDelete(db.Util.createdBy, refresh)
      RefreshesDao.findAll(guid = Some(refresh.guid), isDeleted = None).map(_.guid) should be(Seq(refresh.guid))
      RefreshesDao.findAll(guid = Some(refresh.guid), isDeleted = Some(true)).map(_.guid) should be(Seq(refresh.guid))
      RefreshesDao.findAll(guid = Some(refresh.guid), isDeleted = Some(false)) should be(Nil)
    }

  }

}
