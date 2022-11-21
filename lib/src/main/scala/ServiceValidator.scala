package lib

import cats.data.ValidatedNec

trait ServiceValidator[T] {

  def validate(): ValidatedNec[String, T]

}
