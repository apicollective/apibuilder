package lib

import cats.implicits._
import cats.data.ValidatedNec

trait ValidatedHelpers {
  def sequence(all: Iterable[ValidatedNec[String, Any]]): ValidatedNec[String, Unit] = {
    all.toSeq.sequence.map(_ => ())
  }
}