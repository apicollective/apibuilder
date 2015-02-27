package lib

object Misc {

  def isValidEmail(email: String): Boolean = {
    email.split("@").toList match {
      case username :: domain :: Nil => true
      case _ => false
    }
  }

}
