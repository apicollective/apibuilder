package util

import db.generators.GeneratorHelpers
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.concurrent.ExecutionContext.Implicits.global

class GeneratorServiceUtilSpec extends PlaySpec with GuiceOneAppPerSuite with GeneratorHelpers {

  private[this] def util: GeneratorServiceUtil = app.injector.instanceOf[GeneratorServiceUtil]

  "syncAll" in {
    val s1 = createGeneratorService()
    val s2 = createGeneratorService()
    util.syncAll(pageSize = 10)
  }
}