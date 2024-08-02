package util

import db.Authorization
import db.generators.GeneratorHelpers
import modules.clients.MockGeneratorsData
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class GeneratorServiceUtilSpec extends PlaySpec with GuiceOneAppPerSuite with GeneratorHelpers {

  private def util: GeneratorServiceUtil = app.injector.instanceOf[GeneratorServiceUtil]
  private val data: MockGeneratorsData = app.injector.instanceOf[MockGeneratorsData]

  "syncAll" in {
    val s1 = createGeneratorService()
    val g1 = makeGenerator()
    val s2 = createGeneratorService()
    val g2 = makeGenerator()
    data.add(s1.uri, g1)
    data.add(s2.uri, g2)

    def find(serviceGuid: UUID) = {
      generatorsDao.findAll(
        serviceGuid = Some(serviceGuid),
        limit = Some(1)
      ).headOption
    }

    find(s1.guid) mustBe None

    util.syncAll(pageSize = 1)
    find(s1.guid).value.serviceGuid mustBe s1.guid
    find(s2.guid).value.serviceGuid mustBe s2.guid
  }
}