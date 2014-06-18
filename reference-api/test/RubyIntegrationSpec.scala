import scala.sys.process._

import java.io.File

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

/**
 * Integration spec for generated clients against the reference API.
 */
@RunWith(classOf[JUnitRunner])
class RubyIntegrationSpec extends Specification {
  implicit class SuccessfulProcess(b: ProcessBuilder) {
    def success: Unit = b.! must equalTo(0)
  }

  "Application" should {
    "support the ruby client" in new WithServer {
      val ruby = new File("ruby")

      Process(
        Seq("bundle", "install", "--binstubs=bin", "--path=gems"),
        cwd = ruby
      ).success

      Process(
        Seq("bin/rspec", "client_spec.rb"),
        cwd = ruby,
        "PORT" -> port.toString
      ).success
    }
  }
}
