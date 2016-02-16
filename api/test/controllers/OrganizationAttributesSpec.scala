package controllers

import db.OrganizationsDao
import com.bryzek.apidoc.api.v0.models.{AttributeValueForm, Visibility}
import com.bryzek.apidoc.api.v0.errors.{ErrorsResponse, UnitResponse}
import java.util.UUID

import play.api.test._
import play.api.test.Helpers._

class OrganizationAttributesSpec extends BaseSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val org = createOrganization()

  "POST /organizations/:key/attributes" in new WithServer {
    val attribute = createAttribute()
    val form = AttributeValueForm(
      attributeGuid = attribute.guid,
      value = "test"
    )

    val value = await(
      client.organizations.postAttributesByKey(org.key, form)
    )
    value.attribute.guid must be(attribute.guid)
    value.attribute.name must be(attribute.name)
    value.value must be("test")
  }

  "POST /organizations/:key/attributes validates value" in new WithServer {
    val attribute = createAttribute()
    val form = AttributeValueForm(
      attributeGuid = attribute.guid,
      value = "   "
    )

    intercept[ErrorsResponse] {
      await(client.organizations.postAttributesByKey(org.key, form))
    }.errors.map(_.message) must be(Seq(s"Value is required"))
  }

  "POST /organizations/:key/attributes validates attribute" in new WithServer {
    val form = AttributeValueForm(
      attributeGuid = UUID.randomUUID,
      value = "test"
    )

    intercept[ErrorsResponse] {
      await(client.organizations.postAttributesByKey(org.key, form))
    }.errors.map(_.message) must be(Seq("Attribute not found"))
  }

  "POST /organizations/:key/attributes validates duplicate" in new WithServer {
    val attribute = createAttribute()
    val form = AttributeValueForm(
      attributeGuid = attribute.guid,
      value = "test"
    )

    await(client.organizations.postAttributesByKey(org.key, form))

    intercept[ErrorsResponse] {
      await(client.organizations.postAttributesByKey(org.key, form))
    }.errors.map(_.message) must be(Seq("Value for this attribute already exists"))
  }
  
  "GET /organizations/:key/attributes" in new WithServer {
    val org = createOrganization()
    val attribute1 = createAttribute()
    val attribute2 = createAttribute()

    await(client.organizations.postAttributesByKey(org.key, AttributeValueForm(attribute1.guid, "a")))
    await(client.organizations.postAttributesByKey(org.key, AttributeValueForm(attribute2.guid, "b")))

    await(client.organizations.getAttributesByKey(org.key)).map(_.value) must be(Seq("a", "b"))
  }

  "GET /organizations/:key/attributes w/ name filter" in new WithServer {
    val org = createOrganization()
    val attribute1 = createAttribute()
    val attribute2 = createAttribute()

    await(client.organizations.postAttributesByKey(org.key, AttributeValueForm(attribute1.guid, "a")))
    await(client.organizations.postAttributesByKey(org.key, AttributeValueForm(attribute2.guid, "b")))

    await(client.organizations.getAttributesByKey(org.key, name = Some(attribute1.name))).map(_.value) must be(Seq("a"))
    await(client.organizations.getAttributesByKey(org.key, name = Some(attribute2.name))).map(_.value) must be(Seq("b"))
    await(client.organizations.getAttributesByKey(org.key, name = Some("other"))).map(_.value) must be(Nil)
  }

  "GET /organizations/:key/attributes/:name" in new WithServer {
    val org = createOrganization()
    val attribute1 = createAttribute()
    val attribute2 = createAttribute()

    await(client.organizations.postAttributesByKey(org.key, AttributeValueForm(attribute1.guid, "a")))
    await(client.organizations.postAttributesByKey(org.key, AttributeValueForm(attribute2.guid, "b")))

    await(client.organizations.getAttributesByKeyAndName(org.key, attribute1.name)).value must be("a")
    await(client.organizations.getAttributesByKeyAndName(org.key, attribute2.name)).value must be("b")

    intercept[UnitResponse] {
      await(client.organizations.getAttributesByKeyAndName(org.key, "other"))
    }.status must be(404)
  }

}
