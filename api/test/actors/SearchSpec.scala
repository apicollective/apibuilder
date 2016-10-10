package actors

import db.Authorization
import com.bryzek.apidoc.api.v0.models.ApplicationSummary
import org.scalatest.{FunSpec, Matchers}
import java.util.UUID

class SearchSpec extends FunSpec with Matchers with util.TestApplication {

  describe("indexApplication") {

    it("q") {
      val description = UUID.randomUUID.toString
      val form = db.Util.createApplicationForm().copy(description = Some(description))
      val app = db.Util.createApplication(form = form)
      Search.indexApplication(app.guid)

      Seq(
        app.name,
        app.key,
        description
      ).foreach { query =>
        db.itemsDao.findAll(Authorization.All, q = Some(query)).map(_.guid) should be(Seq(app.guid))
        db.itemsDao.findAll(Authorization.All, q = Some(s"   $query   ")).map(_.guid) should be(Seq(app.guid))
        db.itemsDao.findAll(Authorization.All, q = Some(query.toUpperCase)).map(_.guid) should be(Seq(app.guid))
      }

      db.itemsDao.findAll(Authorization.All, q = Some(UUID.randomUUID.toString)) should be(Nil)
    }

    it("on create") {
      val app = db.Util.createApplication()

      Search.indexApplication(app.guid)

      db.itemsDao.findAll(Authorization.All, 
        guid = Some(app.guid)
      ).map(_.label) should be(Seq(s"${app.organization.key}/${app.key}"))
    }

    it("on update") {
      val form = db.Util.createApplicationForm()
      val app = db.Util.createApplication(form = form)

      Search.indexApplication(app.guid)

      val newName = app.name + "2"

      db.applicationsDao.update(
        updatedBy = db.Util.createdBy,
        app = app,
        form = form.copy(name = newName)
      )

      Search.indexApplication(app.guid)

      val existing = db.applicationsDao.findByGuid(Authorization.All, app.guid).get

      db.itemsDao.findAll(Authorization.All, 
        guid = Some(app.guid),
        q = Some(newName)
      ).size should be(1)
    }

    it("on delete") {
      val app = db.Util.createApplication()
      Search.indexApplication(app.guid)

      db.applicationsDao.softDelete(db.Util.createdBy, app)
      Search.indexApplication(app.guid)
      db.itemsDao.findAll(Authorization.All, guid = Some(app.guid)) should be(Nil)
    }

  }

}
