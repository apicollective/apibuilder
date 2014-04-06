package db

import org.scalatest.FlatSpec
import org.junit.Assert._
import java.util.UUID

class OrganizationSpec extends FlatSpec {

  it should "create" in {
    assertEquals(Util.gilt.name, "Gilt")
    assertEquals(Util.gilt.key, "gilt")
  }
/*
  it should "find by guid" in {
    assertEquals(Organization.findByUserAndGuid(Util.createdBy, Util.gilt.guid).get.guid, Util.gilt.guid)
  }

  it should "find by key" in {
    assertEquals(Organization.findByUserAndKey(Util.createdBy, Util.gilt.key).get.key, Util.gilt.key)
  }
*/
}
