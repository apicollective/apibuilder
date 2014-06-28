package db

import org.scalatest.FlatSpec
import org.junit.Assert._
import java.util.UUID

class UserDaoSpec extends FlatSpec {
  new play.core.StaticApplication(new java.io.File("."))

  it should "upsert" in {
    val user1 = Util.upsertUser("michael@mailinator.com")
    val user2 = Util.upsertUser("michael@mailinator.com")
    assertEquals(user1.guid, user2.guid)
  }

  it should "create different records for different emails" in {
    val user1 = Util.upsertUser("michael@mailinator.com")
    val user2 = Util.upsertUser("other@mailinator.com")
    assertNotEquals(user1.guid, user2.guid)
  }

  it should "return empty list without error guid is not a valid format" in {
    assertEquals(None, UserDao.findByGuid("ASDF"))
  }

  it should "findByGuid" in {
    val user = Util.upsertUser("michael@mailinator.com")
    assertEquals(None, UserDao.findByGuid(UUID.randomUUID.toString))
    assertEquals(Some(user), UserDao.findByGuid(user.guid))
  }

}
