package processor

import cats.implicits._
import cats.data.ValidatedNec

sealed trait TaskType
object TaskType {
  case object Noop extends TaskType { override def toString = "noop" }
  val all: Seq[TaskType] = Seq(Noop)

  private[this] val byString = all.map { t => t.toString.toLowerCase -> t }.toMap
  def fromString(value: String): ValidatedNec[String, TaskType] = {
    byString.get(value.trim.toLowerCase()).toValidNec(s"Invalid task type '$value'")
  }
}