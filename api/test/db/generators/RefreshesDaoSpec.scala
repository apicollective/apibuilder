package db.generators

import org.scalatest.{FunSpec, Matchers}
import java.util.UUID

class RefreshesDaoSpec extends FunSpec with Matchers {

  new play.core.StaticApplication(new java.io.File("."))

  it("upsert") {
    val service = Util.createGeneratorService()
    RefreshesDao.findAll(serviceGuid = Some(service.guid)) should be(Nil)

    RefreshesDao.upsert(db.Util.createdBy, service)
    val refresh = RefreshesDao.findAll(serviceGuid = Some(service.guid)).headOption.getOrElse {
      sys.error("Failed to create refresh record")
    }

    refresh.service.guid should be(service.guid)

    RefreshesDao.upsert(db.Util.createdBy, service)
    val second = RefreshesDao.findAll(serviceGuid = Some(service.guid)).headOption.getOrElse {
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

    it("serviceGuid") {
      val refresh = Util.createGeneratorRefresh()
      RefreshesDao.findAll(serviceGuid = Some(refresh.service.guid)).map(_.guid) should be(Seq(refresh.guid))
      RefreshesDao.findAll(serviceGuid = Some(UUID.randomUUID)).map(_.guid) should be(Nil)
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
