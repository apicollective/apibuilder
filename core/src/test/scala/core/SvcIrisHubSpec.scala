package core

import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FunSpec}
import org.scalatest.Matchers

class SvcIrisHubSpec extends FunSpec with Matchers {

  val Filenames = Seq("svc-iris-hub-0-0-1.json")

  it("should parse valid json") {
    Filenames.foreach { name =>
      val path = s"core/src/test/files/${name}"
      val contents = scala.io.Source.fromFile(path).getLines.mkString("\n")
      val validator = ServiceDescriptionValidator(contents)
      if (!validator.isValid) {
        fail(s"Error parsing json file ${path}:\n  - " + validator.errors.mkString("\n  - "))
      }
    }
  }

}
