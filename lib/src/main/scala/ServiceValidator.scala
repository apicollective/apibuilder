package lib

trait ServiceValidator[T] {

  def validate(): Either[Seq[String], T]

  def errors() = validate match {
    case Left(errors) => errors
    case Right(_) => Nil
  }

  def isValid: Boolean = errors.isEmpty

}
