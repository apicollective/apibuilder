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
      createAttribute(createAttributeForm(name = "a bad key"))
    }.errors.map(_.message) must be(Seq(s"Name must be in all lower case and contain alphanumerics only (-, _, and . are supported). A valid name would be: a-bad-key"))
  }

  /*
  "DELETE /attributes/:key" in new WithServer {
    val attr = createAttribute()
    await(client.attributes.deleteByKey(attr.key)) must be(())
    await(client.attributes.get(key = Some(attr.key))) must be(Seq.empty)
  }

  "GET /attributes" in new WithServer {
    val attr1 = createAttribute()
    val attr2 = createAttribute()

    await(client.attributes.get(key = Some(UUID.randomUUID.toString))) must be(Seq.empty)
    await(client.attributes.get(key = Some(attr1.key))).head.guid must be(attr1.guid)
    await(client.attributes.get(key = Some(attr2.key))).head.guid must be(attr2.guid)
  }

  "GET /attributes/:key" in new WithServer {
    val attr = createAttribute()
    await(client.attributes.getByKey(attr.key)).guid must be(attr.guid)
    intercept[UnitResponse] {
      await(client.attributes.getByKey(UUID.randomUUID.toString))
    }.status must be(404)
  }

  "GET /attributes for an anonymous user shows only public attrs" in new WithServer {
    val privateAttr = createAttribute(createAttributeForm().copy(visibility = Visibility.Attribute))
    val publicAttr = createAttribute(createAttributeForm().copy(visibility = Visibility.Public))
    val anonymous = createUser()

    val client = newClient(anonymous)
    intercept[UnitResponse] {
      await(client.attributes.getByKey(privateAttr.key))
    }.status must be(404)
    await(client.attributes.getByKey(publicAttr.key)).key must be(publicAttr.key)

    await(client.attributes.get(key = Some(privateAttr.key))) must be(Seq.empty)
    await(client.attributes.get(key = Some(publicAttr.key))).map(_.key) must be(Seq(publicAttr.key))
  }
   */

}
