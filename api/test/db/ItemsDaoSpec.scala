package db

import com.gilt.apidoc.api.v0.models.{Item, ItemType}
import org.scalatest.{FunSpec, Matchers}
import org.postgresql.util.PSQLException
import java.util.UUID
import anorm._
import play.api.db._
import play.api.Play.current

class ItemsDaoSpec extends FunSpec with Matchers {

  new play.core.StaticApplication(new java.io.File("."))

  private def upsertItem(
    organizationGuid: UUID = Util.createOrganization().guid,
    guid: UUID = UUID.randomUUID,
    `type`: ItemType = ItemType.Application,
    label: String = "Test",
    description: Option[String] = None,
    content: String = "test"
  ): Item = {
    ItemsDao.upsert(
      guid = guid,
      organizationGuid = organizationGuid,
      `type` = `type`,
      label = label,
      description = description,
      content = content
    )

    ItemsDao.findAll(guid = Some(guid)).headOption.getOrElse {
      sys.error("Failed to upsert item")
    }
  }

  it("upsert") {
    val item = upsertItem()
    ItemsDao.findAll(guid = Some(item.guid)).map(_.guid) should be(Seq(item.guid))
  }

  it("upsert updates content") {
    val guid = UUID.randomUUID
    
    upsertItem(guid = guid, label = "foo")
    ItemsDao.findAll(guid = Some(guid)).map(_.label) should be(Seq("foo"))

    upsertItem(guid = guid, label = "bar")
    ItemsDao.findAll(guid = Some(guid)).map(_.label) should be(Seq("bar"))
  }

  it("delete") {
    val item = upsertItem()
    ItemsDao.findAll(guid = Some(item.guid)).map(_.guid) should be(Seq(item.guid))
    ItemsDao.delete(item.guid)
    ItemsDao.findAll(guid = Some(item.guid)).map(_.guid) should be(Nil)
  }

  describe("findAll") {

    it("orgKey") {
      val org = Util.createOrganization()
      val item = upsertItem(organizationGuid = org.guid)
      ItemsDao.findAll(orgKey = Some(org.key)).map(_.guid) should be(Seq(item.guid))
      ItemsDao.findAll(orgKey = Some(org.key + "2")) should be(Nil)
    }

    it("type") {
      val guid = UUID.randomUUID
    
      upsertItem(guid = guid, `type` = ItemType.Application)
      ItemsDao.findAll(guid = Some(guid), `type` = Some(ItemType.Application)).map(_.guid) should be(Seq(guid))
      ItemsDao.findAll(guid = Some(guid), `type` = Some(ItemType.UNDEFINED("foo"))).map(_.guid) should be(Nil)
    }
    
    it("q") {
      val guid = UUID.randomUUID
    
      upsertItem(guid = guid, content = "foo")
      ItemsDao.findAll(guid = Some(guid), q = Some("foo")).map(_.guid) should be(Seq(guid))

      upsertItem(guid = guid, content = "bar")
      ItemsDao.findAll(guid = Some(guid), q = Some("foo")).map(_.guid) should be(Nil)
      ItemsDao.findAll(guid = Some(guid), q = Some("bar")).map(_.guid) should be(Seq(guid))
    }

    it("limit and offset") {
      val keyword = UUID.randomUUID.toString

      val item1 = upsertItem(label = "A", content = keyword)
      val item2 = upsertItem(label = "b", content = keyword)
      val item3 = upsertItem(label = "C", content = keyword)

      ItemsDao.findAll(q = Some(keyword), limit = 1, offset = 0).map(_.guid) should be(Seq(item1.guid))
      ItemsDao.findAll(q = Some(keyword), limit = 2, offset = 0).map(_.guid) should be(Seq(item1.guid, item2.guid))
      ItemsDao.findAll(q = Some(keyword), limit = 2, offset = 2).map(_.guid) should be(Seq(item3.guid))
    }

  }
}
