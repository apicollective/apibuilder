package util

import java.util.UUID

case class IdGenerator() {

  def randomId(): String = {
    UUID.randomUUID().toString
  }

}
