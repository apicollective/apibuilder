package db

import java.util.UUID

import io.apibuilder.api.v0.models.{ApplicationSummary, Item, Organization}
import io.apibuilder.common.v0.models.Reference
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class ItemsDaoSpec extends PlaySpec with GuiceOneAppPerSuite with db.Helpers {

  private def upsertItem(
    org: InternalOrganization = createOrganization(),
    guid: UUID = UUID.randomUUID,
    label: String = "Test",
    description: Option[String] = None,
    content: String = "test"
  ): Item = {
    val app = createApplication(org = org)
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

  "upsert" in {
    val item = upsertItem()
    itemsDao.findAll(Authorization.All, guid = Some(item.guid)).map(_.guid) must be(Seq(item.guid))
  }

  "upsert updates content" in {
    val guid = UUID.randomUUID
    
    upsertItem(guid = guid, label = "foo")
    itemsDao.findAll(Authorization.All, guid = Some(guid)).map(_.label) must be(Seq("foo"))

    upsertItem(guid = guid, label = "bar")
    itemsDao.findAll(Authorization.All, guid = Some(guid)).map(_.label) must be(Seq("bar"))
  }

  "delete" in {
    val item = upsertItem()
    itemsDao.findAll(Authorization.All, guid = Some(item.guid)).map(_.guid) must be(Seq(item.guid))
    itemsDao.delete(item.guid)
    itemsDao.findAll(Authorization.All, guid = Some(item.guid)).map(_.guid) must be(Nil)
  }

  "findAll" must {

    "q" must {

      "keywords" in {
        val guid = UUID.randomUUID
    
        upsertItem(guid = guid, content = "foo")
        itemsDao.findAll(Authorization.All, guid = Some(guid), q = Some("foo")).map(_.guid) must be(Seq(guid))

        upsertItem(guid = guid, content = "bar")
        itemsDao.findAll(Authorization.All, guid = Some(guid), q = Some("foo")).map(_.guid) must be(Nil)
        itemsDao.findAll(Authorization.All, guid = Some(guid), q = Some("bar")).map(_.guid) must be(Seq(guid))
      }

      "orgKey" in {
        val org = createOrganization()
        val item = upsertItem(org)
        val guids = itemsDao.findAll(Authorization.All, q = Some(s"org:${org.key}")).map(_.guid)
        guids.contains(item.guid) must be(true)
        itemsDao.findAll(Authorization.All, q = Some(s"org:${org.key}2")) must be(Nil)
      }
    }

    "limit and offset" in {
      val keyword = UUID.randomUUID.toString

      val item1 = upsertItem(label = "A", content = keyword)
      val item2 = upsertItem(label = "b", content = keyword)
      val item3 = upsertItem(label = "C", content = keyword)

      itemsDao.findAll(Authorization.All, q = Some(keyword), limit = 1, offset = 0).map(_.guid) must be(Seq(item1.guid))
      itemsDao.findAll(Authorization.All, q = Some(keyword), limit = 2, offset = 0).map(_.guid) must be(Seq(item1.guid, item2.guid))
      itemsDao.findAll(Authorization.All, q = Some(keyword), limit = 2, offset = 2).map(_.guid) must be(Seq(item3.guid))
    }

  }
}
