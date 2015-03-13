package lib

trait ServiceValidator[T] {

  def validate(): Either[Seq[String], T]
  def errors(): Seq[String]
  def isValid: Boolean = errors.isEmpty

}
