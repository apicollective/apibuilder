package lib

import cats.data.ValidatedNec

trait ServiceValidator[T] {

  def validate(rawInput: String): ValidatedNec[String, T]

}
