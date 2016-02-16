package controllers

import db.AttributesDao
import com.bryzek.apidoc.api.v0.models.{Attribute, AttributeForm}
import com.bryzek.apidoc.api.v0.errors.{ErrorsResponse, UnitResponse}
import java.util.UUID

import play.api.test._
import play.api.test.Helpers._

class AttributesSpec extends BaseSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  "POST /attributes" in new WithServer {
    val name = UUID.randomUUID.toString
    val attr = createAttribute(createAttributeForm(name = name, description = Some("foo")))
    attr.name must be(name)
    attr.description must be(Some("foo"))
  }

  "POST /attributes trims whitespace in name" in new WithServer {
    val name = UUID.randomUUID.toString
    val attr = createAttribute(createAttributeForm(name = " " + name + " "))
    attr.name must be(name)
  }

  "POST /attributes validates name is valid" in new WithServer {
    intercept[ErrorsResponse] {
      createAttribute(createAttributeForm(name = "a"))
    }.errors.map(_.message) must be(Seq(s"Name must be at least 4 characters"))

    intercept[ErrorsResponse] {
      createAttribute(createAttributeForm(name = "a bad name"))
    }.errors.map(_.message) must be(Seq(s"Name must be in all lower case and contain alphanumerics only (-, _, and . are supported). A valid name would be: a-bad-name"))
  }

  "DELETE /attributes/:guid" in new WithServer {
    val attr = createAttribute()
    await(client.attributes.deleteByGuid(attr.guid)) must be(())
    await(client.attributes.get(guid = Some(attr.guid))) must be(Seq.empty)
  }

  "DELETE /attributes/:guid returns 401 if different user attempts to delete" in new WithServer {
    val attr = createAttribute()

    val otherClient = newClient(createUser())
    intercept[UnitResponse] {
      await(otherClient.attributes.deleteByGuid(attr.guid))
    }.status must be(401)
  }

  "GET /attributes by guid" in new WithServer {
    val attr1 = createAttribute()
    val attr2 = createAttribute()

    await(client.attributes.get(guid = Some(UUID.randomUUID))) must be(Seq.empty)
    await(client.attributes.get(guid = Some(attr1.guid))).head.guid must be(attr1.guid)
    await(client.attributes.get(guid = Some(attr2.guid))).head.guid must be(attr2.guid)
  }

  "GET /attributes by name" in new WithServer {
    val attr1 = createAttribute()
    val attr2 = createAttribute()

    await(client.attributes.get(name = Some(UUID.randomUUID.toString))) must be(Seq.empty)
    await(client.attributes.get(name = Some(attr1.name))).head.guid must be(attr1.guid)
    await(client.attributes.get(name = Some(attr2.name))).head.guid must be(attr2.guid)
  }

  "GET /attributes/:guid" in new WithServer {
    val attr = createAttribute()
    await(client.attributes.getByGuid(attr.guid)).guid must be(attr.guid)
    intercept[UnitResponse] {
      await(client.attributes.getByGuid(UUID.randomUUID))
    }.status must be(404)
  }

}
