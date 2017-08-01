package controllers

import io.apibuilder.api.v0.models.AttributeValueForm
import play.api.test._

class OrganizationAttributesSpec extends PlaySpecification with MockClient {

  import scala.concurrent.ExecutionContext.Implicits.global

  private[this] lazy val org = createOrganization()

  "PUT /organizations/:key/attributes" in new WithServer(port=defaultPort) {
    val attribute = createAttribute()
    val form = AttributeValueForm(value = "test")

    val attributeValue = await(
      client.organizations.putAttributesByKeyAndName(org.key, attribute.name, form)
    )
    attributeValue.attribute.guid must beEqualTo(attribute.guid)
    attributeValue.attribute.name must beEqualTo(attribute.name)
    attributeValue.value must beEqualTo("test")
  }

  "PUT /organizations/:key/attributes validates value" in new WithServer(port=defaultPort) {
    val attribute = createAttribute()
    val form = AttributeValueForm(value = "   ")

    expectErrors {
      client.organizations.putAttributesByKeyAndName(org.key, attribute.name, form)
    }.errors.map(_.message) must beEqualTo(Seq(s"Value is required"))
  }

  "PUT /organizations/:key/attributes validates attribute" in new WithServer(port=defaultPort) {
    val form = AttributeValueForm(value = "rest")

    expectNotFound {
      client.organizations.putAttributesByKeyAndName(org.key, createRandomName("attr"), form)
    }
  }

  "PUT /organizations/:key/attributes updates existing value" in new WithServer(port=defaultPort) {
    val attribute = createAttribute()

    val form = AttributeValueForm(value = "test")
    val original = await(client.organizations.putAttributesByKeyAndName(org.key, attribute.name, form))
    original.value must beEqualTo("test")

    val form2 = AttributeValueForm(value = "test2")
    val updated = await(client.organizations.putAttributesByKeyAndName(org.key, attribute.name, form2))
    updated.value must beEqualTo("test2")
  }
  
  "GET /organizations/:key/attributes" in new WithServer(port=defaultPort) {
    val org = createOrganization()
    val attribute1 = createAttribute()
    val attribute2 = createAttribute()

    await(client.organizations.putAttributesByKeyAndName(org.key, attribute1.name, AttributeValueForm("a")))
    await(client.organizations.putAttributesByKeyAndName(org.key, attribute2.name, AttributeValueForm("b")))

    await(client.organizations.getAttributesByKey(org.key)).map(_.value) must beEqualTo(Seq("a", "b"))
  }

  "GET /organizations/:key/attributes w/ name filter" in new WithServer(port=defaultPort) {
    val org = createOrganization()
    val attribute1 = createAttribute()
    val attribute2 = createAttribute()

    await(client.organizations.putAttributesByKeyAndName(org.key, attribute1.name, AttributeValueForm("a")))
    await(client.organizations.putAttributesByKeyAndName(org.key, attribute2.name, AttributeValueForm("b")))

    await(client.organizations.getAttributesByKey(org.key, name = Some(attribute1.name))).map(_.value) must beEqualTo(Seq("a"))
    await(client.organizations.getAttributesByKey(org.key, name = Some(attribute2.name))).map(_.value) must beEqualTo(Seq("b"))
    await(client.organizations.getAttributesByKey(org.key, name = Some("other"))).map(_.value) must beEqualTo(Nil)
  }

  "GET /organizations/:key/attributes/:name" in new WithServer(port=defaultPort) {
    val org = createOrganization()
    val attribute1 = createAttribute()
    val attribute2 = createAttribute()

    await(client.organizations.putAttributesByKeyAndName(org.key, attribute1.name, AttributeValueForm("a")))
    await(client.organizations.putAttributesByKeyAndName(org.key, attribute2.name, AttributeValueForm("b")))

    await(client.organizations.getAttributesByKeyAndName(org.key, attribute1.name)).value must beEqualTo("a")
    await(client.organizations.getAttributesByKeyAndName(org.key, attribute2.name)).value must beEqualTo("b")

    expectNotFound {
      client.organizations.getAttributesByKeyAndName(org.key, "other")
    }
  }

}
