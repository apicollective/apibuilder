package controllers

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}

class AttributesSpec extends PlaySpec with MockClient with OneServerPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

  "POST /attributes" in {
    val name = createRandomName("attribute")
    val attr = createAttribute(createAttributeForm(name = name, description = Some("foo")))
    attr.name must equal(name)
    attr.description must beSome("foo")
  }

  "POST /attributes trims whitespace in name" in {
    val name = createRandomName("attribute")
    val attr = createAttribute(createAttributeForm(name = " " + name + " "))
    attr.name must equal(name)
  }

  "POST /attributes validates name is valid" in {
    expectErrors {
      client.attributes.post(createAttributeForm(name = "a"))
    }.errors.map(_.message) must equal(Seq(s"Name must be at least 3 characters"))

    expectErrors {
      client.attributes.post(createAttributeForm(name = "a bad name"))
    }.errors.map(_.message) must equal(Seq(s"Name must be in all lower case and contain alphanumerics only (-, _, and . are supported). A valid name would be: a-bad-name"))
  }

  "DELETE /attributes/:name" in {
    val attr = createAttribute()
    await(client.attributes.deleteByName(attr.name)) must equal(())
    await(client.attributes.get(name = Some(attr.name))) must equal(Nil)
  }

  "DELETE /attributes/:name returns 401 if different user attempts to delete" in {
    val attr = createAttribute()

    val otherClient = newClient(createUser())
    expectNotAuthorized {
      otherClient.attributes.deleteByName(attr.name)
    }
  }

  "GET /attributes by name" in {
    val attr1 = createAttribute()
    val attr2 = createAttribute()

    await(client.attributes.get(name = Some(createRandomName("attr")))) must equal(Nil)
    await(client.attributes.get(name = Some(attr1.name))).head.guid must equal(attr1.guid)
    await(client.attributes.get(name = Some(attr2.name))).head.guid must equal(attr2.guid)
  }

  "GET /attributes/:name" in {
    val attr = createAttribute()
    await(client.attributes.getByName(attr.name)).guid must equal(attr.guid)

    expectNotFound {
      client.attributes.getByName(createRandomName("attr"))
    }
  }

}
