package actors

import org.scalatest.{FunSpec, Matchers}

class SearchSpec extends FunSpec with Matchers {

  new play.core.StaticApplication(new java.io.File("."))

  describe("indexApplication") {

    it("on create") {
      val app = db.Util.createApplication()
      Search.indexApplication(app.guid)
    }

  }

}