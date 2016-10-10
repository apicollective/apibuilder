package db

import com.bryzek.apidoc.api.v0.models.{ApplicationSummary, Item, Organization}
import com.bryzek.apidoc.common.v0.models.Reference
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
    itemsDao.upsert(
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

    itemsDao.findAll(Authorization.All, guid = Some(guid)).headOption.getOrElse {
      sys.error("Failed to upsert item")
    }
  }

  it("upsert") {
    val item = upsertItem()
    itemsDao.findAll(Authorization.All, guid = Some(item.guid)).map(_.guid) should be(Seq(item.guid))
  }

  it("upsert updates content") {
    val guid = UUID.randomUUID
    
    upsertItem(guid = guid, label = "foo")
    itemsDao.findAll(Authorization.All, guid = Some(guid)).map(_.label) should be(Seq("foo"))

    upsertItem(guid = guid, label = "bar")
    itemsDao.findAll(Authorization.All, guid = Some(guid)).map(_.label) should be(Seq("bar"))
  }

  it("delete") {
    val item = upsertItem()
    itemsDao.findAll(Authorization.All, guid = Some(item.guid)).map(_.guid) should be(Seq(item.guid))
    itemsDao.delete(item.guid)
    itemsDao.findAll(Authorization.All, guid = Some(item.guid)).map(_.guid) should be(Nil)
  }

  describe("findAll") {

    describe("q") {

      it("keywords") {
        val guid = UUID.randomUUID
    
        upsertItem(guid = guid, content = "foo")
        itemsDao.findAll(Authorization.All, guid = Some(guid), q = Some("foo")).map(_.guid) should be(Seq(guid))

        upsertItem(guid = guid, content = "bar")
        itemsDao.findAll(Authorization.All, guid = Some(guid), q = Some("foo")).map(_.guid) should be(Nil)
        itemsDao.findAll(Authorization.All, guid = Some(guid), q = Some("bar")).map(_.guid) should be(Seq(guid))
      }

      it("orgKey") {
        val org = Util.createOrganization()
        val item = upsertItem(org = org)
        val guids = itemsDao.findAll(Authorization.All, q = Some(s"org:${org.key}")).map(_.guid)
        guids.contains(item.guid) should be(true)
        itemsDao.findAll(Authorization.All, q = Some(s"org:${org.key}2")) should be(Nil)
      }
    }

    it("limit and offset") {
      val keyword = UUID.randomUUID.toString

      val item1 = upsertItem(label = "A", content = keyword)
      val item2 = upsertItem(label = "b", content = keyword)
      val item3 = upsertItem(label = "C", content = keyword)

      itemsDao.findAll(Authorization.All, q = Some(keyword), limit = 1, offset = 0).map(_.guid) should be(Seq(item1.guid))
      itemsDao.findAll(Authorization.All, q = Some(keyword), limit = 2, offset = 0).map(_.guid) should be(Seq(item1.guid, item2.guid))
      itemsDao.findAll(Authorization.All, q = Some(keyword), limit = 2, offset = 2).map(_.guid) should be(Seq(item3.guid))
    }

  }
}
