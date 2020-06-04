package helpers

import java.util.UUID

trait RandomHelpers {

  def randomString(): String = {
    UUID.randomUUID.toString
  }

  def createRandomName(suffix: String): String = {
    s"z-test-$suffix-" + UUID.randomUUID.toString
  }

}
