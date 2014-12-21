package models

import com.gilt.apidocspec.models.Service
import generator.ScalaService
import core.{ServiceBuilder}
import org.scalatest.{ ShouldMatchers, FunSpec }

class Play2BindablesSpec extends FunSpec with ShouldMatchers {

  lazy val service = TestHelper.parseFile(s"reference-api/api.json").serviceDescription.get
  lazy val ssd = new ScalaService(service)
  lazy val ageGroup = ssd.enums.find(_.name == "AgeGroup").getOrElse {
    sys.error("No age group enum found")
  }

  it("generates bindable for a single enum") {
    TestHelper.assertEqualsFile(
      "test/resources/generators/play-2-bindable-age-group.txt",
      Play2Bindables.buildImplicit(ageGroup)
    )
  }

  it("generates bindable object") {
    TestHelper.assertEqualsFile(
      "test/resources/generators/play-2-bindable-reference-api-object.txt",
      Play2Bindables.build(ssd)
    )
  }

}
