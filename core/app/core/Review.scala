package core

case class Review(key: String, name: String)

object Review {

  val Accept = Review("accept", "Accept")
  val Decline = Review("decline", "Decline")

  def fromString(role: String): Option[Review] = {
    if (Accept.key == role) {
      Some(Accept)
    } else if (Decline.key == role) {
      Some(Decline)
    } else {
      None
    }
  }

}
