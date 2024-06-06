package processor

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class CheckInvariantsProcessorSpec extends PlaySpec with GuiceOneAppPerSuite with db.Helpers {

  private def processor: CheckInvariantsProcessor = injector.instanceOf[CheckInvariantsProcessor]

  "process" in {
    processor.processRecord("periodic")
  }
}
