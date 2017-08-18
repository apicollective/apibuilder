package controllers

import io.apibuilder.api.v0.errors.UnitResponse
import play.api.test._

class AttributesSpec extends PlaySpecification with MockClient {

  import scala.concurrent.ExecutionContext.Implicits.global

  "POST /attributes" in new WithServer(port=defaultPort) {
    val name = createRandomName("attribute")
    val attr = createAttribute(createAttributeForm(name = name, description = Some("foo")))
    attr.name must beEqualTo(name)
    attr.description must beSome("foo")
  }

  "POST /attributes trims whitespace in name" in new WithServer(port=defaultPort) {
    val name = createRandomName("attribute")
    val attr = createAttribute(createAttributeForm(name = " " + name + " "))
    attr.name must beEqualTo(name)
  }

  "POST /attributes validates name is valid" in new WithServer(port=defaultPort) {
    expectErrors {
      client.attributes.post(createAttributeForm(name = "a"))
    }.errors.map(_.message) must beEqualTo(Seq(s"Name must be at least 3 characters"))

    expectErrors {
      client.attributes.post(createAttributeForm(name = "a bad name"))
    }.errors.map(_.message) must beEqualTo(Seq(s"Name must be in all lower case and contain alphanumerics only (-, _, and . are supported). A valid name would be: a-bad-name"))
  }

  "DELETE /attributes/:name" in new WithServer(port=defaultPort) {
    val attr = createAttribute()
    await(client.attributes.deleteByName(attr.name)) must beEqualTo(())
    await(client.attributes.get(name = Some(attr.name))) must beEqualTo(Nil)
  }

  "DELETE /attributes/:name returns 401 if different user attempts to delete" in new WithServer(port=defaultPort) {
    val attr = createAttribute()

    val otherClient = newClient(createUser())
    expectNotAuthorized {
      otherClient.attributes.deleteByName(attr.name)
    }
  }

  "GET /attributes by name" in new WithServer(port=defaultPort) {
    val attr1 = createAttribute()
    val attr2 = createAttribute()

    await(client.attributes.get(name = Some(createRandomName("attr")))) must beEqualTo(Nil)
    await(client.attributes.get(name = Some(attr1.name))).head.guid must beEqualTo(attr1.guid)
    await(client.attributes.get(name = Some(attr2.name))).head.guid must beEqualTo(attr2.guid)
  }

  "GET /attributes/:name" in new WithServer(port=defaultPort) {
    val attr = createAttribute()
    await(client.attributes.getByName(attr.name)).guid must beEqualTo(attr.guid)

    expectNotFound {
      client.attributes.getByName(createRandomName("attr"))
    }
  }

}
