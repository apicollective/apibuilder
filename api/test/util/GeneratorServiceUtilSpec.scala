package util

import db.Authorization
import db.generators.GeneratorHelpers
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class GeneratorServiceUtilSpec extends PlaySpec with GuiceOneAppPerSuite with GeneratorHelpers {

  private[this] def util: GeneratorServiceUtil = app.injector.instanceOf[GeneratorServiceUtil]

  "syncAll" in {
    val s1 = createGeneratorService()
    val s2 = createGeneratorService()

    def find(serviceGuid: UUID) = {
      generatorsDao.findAll(
        authorization = Authorization.All,
        serviceGuid = Some(serviceGuid),
        limit = 1
      ).headOption
    }

    find(s1.guid) mustBe None

    util.syncAll(pageSize = 10)
    find(s1.guid).value.service.guid mustBe s1.guid
  }
}