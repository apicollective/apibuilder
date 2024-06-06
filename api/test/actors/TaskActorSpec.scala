package actors

import db.Authorization
import helpers.AsyncHelpers
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class TaskActorSpec extends PlaySpec with GuiceOneAppPerSuite with AsyncHelpers with db.Helpers {

  "run" in {
    val app = createApplication()
    eventuallyInNSeconds(10) {
      itemsDao.findByGuid(Authorization.All, app.guid).value
    }
  }

}
