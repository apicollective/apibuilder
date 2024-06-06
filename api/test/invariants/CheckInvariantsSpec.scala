package invariants

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class CheckInvariantsSpec extends PlaySpec with GuiceOneAppPerSuite with db.Helpers {

  private def checkInvariants: CheckInvariants = injector.instanceOf[CheckInvariants]

  "process" in {
    checkInvariants.process()
  }
}
