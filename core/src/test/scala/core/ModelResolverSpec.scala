package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class ModelResolverSpec extends FunSpec with Matchers {

  private val guidField = InternalField(name = Some("guid"), datatype = Some("uuid"))
  private val userField = InternalField(name = Some("user"), references = Some(InternalReference("users.guid")))

  private val user = InternalModel(name = "user",
                                   plural = "users",
                                   description = None,
                                   fields = Seq(guidField))

  private val organization = InternalModel(name = "organization",
                                           plural = "organizations",
                                           description = None,
                                           fields = Seq(guidField))

  private val account = InternalModel(name = "account",
                                      plural = "accounts",
                                      description = None,
                                      fields = Seq(userField))

  it("no references") {
    val internal = Seq(user)
    ModelResolver.build(internal).map(_.name) should be(Seq("user"))
  }

  it("multiple models w/ no references") {
    val internal = Seq(user, organization)
    ModelResolver.build(internal).map(_.name).sorted should be(Seq("organization", "user"))
  }

  it("w/ a reference") {
    ModelResolver.build(Seq(account, user)).map(_.name).sorted should be(Seq("account", "user"))
    ModelResolver.build(Seq(user, account)).map(_.name).sorted should be(Seq("account", "user"))
  }

  it("reference to own field") {
    val keyField = InternalField(name = Some("key"), datatype = Some("string"))
    val parentKeyField = InternalField(name = Some("parent_key"), references = Some(InternalReference("categories.key")))

    val category = InternalModel(name = "category",
                                 plural = "categories",
                                 description = None,
                                 fields = Seq(guidField, keyField, parentKeyField))
    ModelResolver.build(Seq(category)).map(_.name) should be(Seq("category"))
  }

  it("throws error on circular reference") {
    val foo = InternalModel(name = "foo",
                            plural = "foos",
                            description = None,
                            fields = Seq(InternalField(name = Some("guid"), datatype = Some("uuid")),
                                         InternalField(name = Some("bar"), references = Some(InternalReference("bars.guid")))))

    val bar = InternalModel(name = "bar",
                            plural = "bars",
                            description = None,
                            fields = Seq(InternalField(name = Some("guid"), datatype = Some("uuid")),
                                         InternalField(name = Some("foo"), references = Some(InternalReference("foos.guid")))))

    // TODO: Scala test validation of exception
    try {
      ModelResolver.build(Seq(user, account, foo, bar))
      fail("No error raised")
    } catch {
      case e: Throwable => {
        e.getMessage should be("Circular dependencies found while trying to resolve references for models: foo bar")
      }
    }
  }

}
