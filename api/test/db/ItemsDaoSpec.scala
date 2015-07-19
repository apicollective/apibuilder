package db

import com.bryzek.apidoc.api.v0.models.{ApplicationSummary, Item, Organization, Reference}
import org.scalatest.{FunSpec, Matchers}
import org.postgresql.util.PSQLException
import java.util.UUID

class ItemsDaoSpec extends FunSpec with Matchers with util.TestApplication {

  private[this] def upsertItem(
    org: Organization = Util.createOrganization(),
    guid: UUID = UUID.randomUUID,
    label: String = "Test",
    description: Option[String] = None,
    content: String = "test"
  ): Item = {
    val app = Util.createApplication(org = org)
    ItemsDao.upsert(
      guid = guid,
      detail = ApplicationSummary(
        guid = app.guid,
        organization = Reference(org.guid, org.key),
        key = app.key
      ),
      label = label,
      description = description,
      content = content
    )

    ItemsDao.findAll(Authorization.All, guid = Some(guid)).headOption.getOrElse {
      sys.error("Failed to upsert item")
    }
  }

  it("upsert") {
    val item = upsertItem()
    ItemsDao.findAll(Authorization.All, guid = Some(item.guid)).map(_.guid) should be(Seq(item.guid))
  }

  it("upsert updates content") {
    val guid = UUID.randomUUID
    
    upsertItem(guid = guid, label = "foo")
    ItemsDao.findAll(Authorization.All, guid = Some(guid)).map(_.label) should be(Seq("foo"))

    upsertItem(guid = guid, label = "bar")
    ItemsDao.findAll(Authorization.All, guid = Some(guid)).map(_.label) should be(Seq("bar"))
  }

  it("delete") {
    val item = upsertItem()
    ItemsDao.findAll(Authorization.All, guid = Some(item.guid)).map(_.guid) should be(Seq(item.guid))
    ItemsDao.delete(item.guid)
    ItemsDao.findAll(Authorization.All, guid = Some(item.guid)).map(_.guid) should be(Nil)
  }

  describe("findAll") {

    describe("q") {

      it("keywords") {
        val guid = UUID.randomUUID
    
        upsertItem(guid = guid, content = "foo")
        ItemsDao.findAll(Authorization.All, guid = Some(guid), q = Some("foo")).map(_.guid) should be(Seq(guid))

        upsertItem(guid = guid, content = "bar")
        ItemsDao.findAll(Authorization.All, guid = Some(guid), q = Some("foo")).map(_.guid) should be(Nil)
        ItemsDao.findAll(Authorization.All, guid = Some(guid), q = Some("bar")).map(_.guid) should be(Seq(guid))
      }

      it("orgKey") {
        val org = Util.createOrganization()
        val item = upsertItem(org = org)
        ItemsDao.findAll(Authorization.All, q = Some(s"org:${org.key}")).map(_.guid) should be(Seq(item.guid))
        ItemsDao.findAll(Authorization.All, q = Some(s"org:${org.key}2")).map(_.guid) should be(Nil)
      }
    }

    it("limit and offset") {
      val keyword = UUID.randomUUID.toString

      val item1 = upsertItem(label = "A", content = keyword)
      val item2 = upsertItem(label = "b", content = keyword)
      val item3 = upsertItem(label = "C", content = keyword)

      ItemsDao.findAll(Authorization.All, q = Some(keyword), limit = 1, offset = 0).map(_.guid) should be(Seq(item1.guid))
      ItemsDao.findAll(Authorization.All, q = Some(keyword), limit = 2, offset = 0).map(_.guid) should be(Seq(item1.guid, item2.guid))
      ItemsDao.findAll(Authorization.All, q = Some(keyword), limit = 2, offset = 2).map(_.guid) should be(Seq(item3.guid))
    }

  }
}
