package db

import org.scalatest.FlatSpec
import org.junit.Assert._
import java.util.UUID

class OrganizationDaoSpec extends FlatSpec {

  it should "create" in {
    assertEquals(Util.gilt.name, "Gilt")
    assertEquals(Util.gilt.key, "gilt")
  }

  it should "find by guid" in {
    assertEquals(OrganizationDao.findByGuid(Util.gilt.guid).get.guid, Util.gilt.guid)
  }

  it should "findAll by key" in {
    assertEquals(OrganizationDao.findAll(key = Some(Util.gilt.key)).head.key, Util.gilt.key)
  }

}
