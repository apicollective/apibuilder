package controllers

import io.apibuilder.api.v0.models.AttributeValueForm
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.PlaySpec

class OrganizationAttributesSpec extends PlaySpec with MockClient with GuiceOneServerPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

  private lazy val org = createOrganization()

  "PUT /organizations/:key/attributes" in {
    val attribute = createAttribute()
    val form = AttributeValueForm(value = "test")

    val attributeValue = await(
      client.organizations.putAttributesByKeyAndName(org.key, attribute.name, form)
    )
    attributeValue.attribute.guid must equal(attribute.guid)
    attributeValue.attribute.name must equal(attribute.name)
    attributeValue.value must equal("test")
  }

  "PUT /organizations/:key/attributes validates value" in {
    val attribute = createAttribute()
    val form = AttributeValueForm(value = "   ")

    expectErrors {
      client.organizations.putAttributesByKeyAndName(org.key, attribute.name, form)
    }.errors.map(_.message) must equal(Seq(s"Value is required"))
  }

  "PUT /organizations/:key/attributes validates attribute" in {
    val form = AttributeValueForm(value = "rest")

    expectNotFound {
      client.organizations.putAttributesByKeyAndName(org.key, createRandomName("attr"), form)
    }
  }

  "PUT /organizations/:key/attributes updates existing value" in {
    val attribute = createAttribute()

    val form = AttributeValueForm(value = "test")
    val original = await(client.organizations.putAttributesByKeyAndName(org.key, attribute.name, form))
    original.value must equal("test")

    val form2 = AttributeValueForm(value = "test2")
    val updated = await(client.organizations.putAttributesByKeyAndName(org.key, attribute.name, form2))
    updated.value must equal("test2")
  }
  
  "GET /organizations/:key/attributes" in {
    val org = createOrganization()
    val attribute1 = createAttribute()
    val attribute2 = createAttribute()

    await(client.organizations.putAttributesByKeyAndName(org.key, attribute1.name, AttributeValueForm("a")))
    await(client.organizations.putAttributesByKeyAndName(org.key, attribute2.name, AttributeValueForm("b")))

    await(client.organizations.getAttributesByKey(org.key)).map(_.value) must equal(Seq("a", "b"))
  }

  "GET /organizations/:key/attributes w/ name filter" in {
    val org = createOrganization()
    val attribute1 = createAttribute()
    val attribute2 = createAttribute()

    await(client.organizations.putAttributesByKeyAndName(org.key, attribute1.name, AttributeValueForm("a")))
    await(client.organizations.putAttributesByKeyAndName(org.key, attribute2.name, AttributeValueForm("b")))

    await(client.organizations.getAttributesByKey(org.key, name = Some(attribute1.name))).map(_.value) must equal(Seq("a"))
    await(client.organizations.getAttributesByKey(org.key, name = Some(attribute2.name))).map(_.value) must equal(Seq("b"))
    await(client.organizations.getAttributesByKey(org.key, name = Some("other"))).map(_.value) must equal(Nil)
  }

  "GET /organizations/:key/attributes/:name" in {
    val org = createOrganization()
    val attribute1 = createAttribute()
    val attribute2 = createAttribute()

    await(client.organizations.putAttributesByKeyAndName(org.key, attribute1.name, AttributeValueForm("a")))
    await(client.organizations.putAttributesByKeyAndName(org.key, attribute2.name, AttributeValueForm("b")))

    await(client.organizations.getAttributesByKeyAndName(org.key, attribute1.name)).value must equal("a")
    await(client.organizations.getAttributesByKeyAndName(org.key, attribute2.name)).value must equal("b")

    expectNotFound {
      client.organizations.getAttributesByKeyAndName(org.key, "other")
    }
  }

}
