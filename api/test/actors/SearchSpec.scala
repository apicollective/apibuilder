package actors

import com.gilt.apidoc.api.v0.models.ItemType
import org.scalatest.{FunSpec, Matchers}

class SearchSpec extends FunSpec with Matchers {

  new play.core.StaticApplication(new java.io.File("."))

  describe("indexApplication") {

    it("on create") {
      val app = db.Util.createApplication()

      Search.indexApplication(app.guid)

      db.ItemsDao.findAll(
        guid = Some(app.guid),
        `type` = Some(ItemType.Application)
      ).map(_.label) should be(Seq(app.name))
    }

    it("on update") {
      val form = db.Util.createApplicationForm()
      val app = db.Util.createApplication(form = form)

      Search.indexApplication(app.guid)

      val newName = app.name + " 2"

      db.ApplicationsDao.update(
        updatedBy = db.Util.createdBy,
        app = app,
        form = form.copy(name = newName)
      )

      Search.indexApplication(app.guid)

      db.ItemsDao.findAll(
        guid = Some(app.guid),
        `type` = Some(ItemType.Application)
      ).map(_.label) should be(Seq(newName))
    }

    it("on delete") {
      val app = db.Util.createApplication()
      Search.indexApplication(app.guid)

      db.ApplicationsDao.softDelete(db.Util.createdBy, app)
      Search.indexApplication(app.guid)
      db.ItemsDao.findAll(guid = Some(app.guid)) should be(Nil)
    }

  }

}
