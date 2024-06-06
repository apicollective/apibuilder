package helpers

import lib.TestHelper
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}

trait AsyncHelpers extends TestHelper with Eventually {

  def eventuallyInNSeconds[T](n: Long = 3)(f: => T): T = {
    eventually(Timeout(Span(n, Seconds))) {
      f
    }
  }

}
