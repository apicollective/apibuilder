package lib

trait ServiceValidator[T] {

  def validate(): Either[Seq[String], T]

}
