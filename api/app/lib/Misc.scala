package lib

object Misc {

  def isValidEmail(email: String): Boolean = {
    email.split("@").toList match {
      case username :: domain :: Nil => !username.trim.isEmpty && !domain.trim.isEmpty
      case _ => false
    }
  }

  def emailDomain(email: String): Option[String] = {
    email.trim.split("@").toList match {
      case _ :: domain :: Nil => {
        Some(domain.toLowerCase.trim).filter(_.nonEmpty)
      }
      case _ => None
    }
  }

}