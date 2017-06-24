package controllers

import db.OrganizationsDao
import io.apibuilder.apidoc.api.v0.models.{AttributeValueForm, Visibility}
import io.apibuilder.apidoc.api.v0.errors.{ErrorsResponse, UnitResponse}

import play.api.test._
import play.api.test.Helpers._

class OrganizationAttributesSpec extends BaseSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val org = createOrganization()

  "PUT /organizations/:key/attributes" in new WithServer {
    val attribute = createAttribute()
    val form = AttributeValueForm(value = "test")

    val value = await(
      client.organizations.putAttributesByKeyAndName(org.key, attribute.name, form)
    )
    value.attribute.guid must be(attribute.guid)
    value.attribute.name must be(attribute.name)
    value.value must be("test")
  }

  "PUT /organizations/:key/attributes validates value" in new WithServer {
    val attribute = createAttribute()
    val form = AttributeValueForm(value = "   ")

    intercept[ErrorsResponse] {
      await(client.organizations.putAttributesByKeyAndName(org.key, attribute.name, form))
    }.errors.map(_.message) must be(Seq(s"Value is required"))
  }

  "PUT /organizations/:key/attributes validates attribute" in new WithServer {
    val form = AttributeValueForm(value = "rest")

    intercept[UnitResponse] {
      await(client.organizations.putAttributesByKeyAndName(org.key, createRandomName("attr"), form))
    }.status must be(404)
  }

  "PUT /organizations/:key/attributes updates existing value" in new WithServer {
    val attribute = createAttribute()

    val form = AttributeValueForm(value = "test")
    val original = await(client.organizations.putAttributesByKeyAndName(org.key, attribute.name, form))
    original.value must be("test")

    val form2 = AttributeValueForm(value = "test2")
    val updated = await(client.organizations.putAttributesByKeyAndName(org.key, attribute.name, form2))
    updated.value must be("test2")
  }
  
  "GET /organizations/:key/attributes" in new WithServer {
    val org = createOrganization()
    val attribute1 = createAttribute()
    val attribute2 = createAttribute()

    await(client.organizations.putAttributesByKeyAndName(org.key, attribute1.name, AttributeValueForm("a")))
    await(client.organizations.putAttributesByKeyAndName(org.key, attribute2.name, AttributeValueForm("b")))

    await(client.organizations.getAttributesByKey(org.key)).map(_.value) must be(Seq("a", "b"))
  }

  "GET /organizations/:key/attributes w/ name filter" in new WithServer {
    val org = createOrganization()
    val attribute1 = createAttribute()
    val attribute2 = createAttribute()

    await(client.organizations.putAttributesByKeyAndName(org.key, attribute1.name, AttributeValueForm("a")))
    await(client.organizations.putAttributesByKeyAndName(org.key, attribute2.name, AttributeValueForm("b")))

    await(client.organizations.getAttributesByKey(org.key, name = Some(attribute1.name))).map(_.value) must be(Seq("a"))
    await(client.organizations.getAttributesByKey(org.key, name = Some(attribute2.name))).map(_.value) must be(Seq("b"))
    await(client.organizations.getAttributesByKey(org.key, name = Some("other"))).map(_.value) must be(Nil)
  }

  "GET /organizations/:key/attributes/:name" in new WithServer {
    val org = createOrganization()
    val attribute1 = createAttribute()
    val attribute2 = createAttribute()

    await(client.organizations.putAttributesByKeyAndName(org.key, attribute1.name, AttributeValueForm("a")))
    await(client.organizations.putAttributesByKeyAndName(org.key, attribute2.name, AttributeValueForm("b")))

    await(client.organizations.getAttributesByKeyAndName(org.key, attribute1.name)).value must be("a")
    await(client.organizations.getAttributesByKeyAndName(org.key, attribute2.name)).value must be("b")

    intercept[UnitResponse] {
      await(client.organizations.getAttributesByKeyAndName(org.key, "other"))
    }.status must be(404)
  }

}
