package lib

case class Review(key: String, name: String)

object Review {

  val Accept = Review("accept", "Accept")
  val Decline = Review("decline", "Decline")

  val All = Seq(Accept, Decline)

  def fromString(key: String): Option[Review] = {
    val lowerKey = key.toLowerCase
    All.find(_.key == lowerKey)
  }

}
