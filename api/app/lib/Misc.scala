package lib

object Misc {

  def isValidEmail(email: String): Boolean = {
    email.split("@").toList match {
      case username :: domain :: Nil => true
      case _ => false
    }
  }

  def emailDomain(email: String): Option[String] = {
    email.trim.split("@").toList match {
      case username :: domain :: Nil => {
        Some(domain.toLowerCase.trim)
      }
      case _ => None
    }
  }


}
