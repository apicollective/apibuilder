package helpers

import akka.util.Timeout
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import org.scalatest.concurrent.Eventually._
import org.scalatest.time.{Seconds, Span}

import java.util.UUID
import scala.concurrent.duration.{FiniteDuration, SECONDS}
import scala.concurrent.{Await, Future}

trait TestHelpers {

  def createTestId(): String = {
    "tst-" + UUID.randomUUID.toString
  }

  def createTestEmail(): String = {
    "tst-" + UUID.randomUUID.toString + "@test.bryzek.com"
  }

  def eventuallyInNSeconds[T](n: Long = 3)(f: => T): T = {
    eventually(timeout(Span(n, Seconds))) {
      f
    }
  }

  def expectRight[T](result: Either[_, T]): T = {
    result match {
      case Left(errors) => sys.error(s"Expected right value but found Left: ${errors}")
      case Right(obj) => obj
    }
  }

  def expectLeft[T](result: Either[T, _]): T = {
    result match {
      case Left(errors) => errors
      case Right(obj) => sys.error(s"Expected left value but found right: ${obj}")
    }
  }

  def expectValid[T](f: => Validated[_, T]): T = {
    f match {
      case Valid(r) => r
      case Invalid(error) => sys.error(s"Expected valid: ${error}")
    }
  }

  def expectInvalid[T](f: => Validated[T, _]): T = {
    f match {
      case Valid(_) => sys.error(s"Expected invalid")
      case Invalid(r) => r
    }
  }

  private[this] implicit val defaultTimeout: Timeout = FiniteDuration(5, SECONDS)

  def await[T](future: Future[T])(implicit timeout: Timeout = defaultTimeout): T = {
    Await.result(future, timeout.duration)
  }
}
