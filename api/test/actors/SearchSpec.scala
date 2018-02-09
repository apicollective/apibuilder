package actors

import db.Authorization
import io.apibuilder.api.v0.models.ApplicationSummary
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import java.util.UUID

class SearchSpec extends PlaySpec with OneAppPerSuite with util.Daos {

  "indexApplication" must {

    "q" in {
      val description = UUID.randomUUID.toString
      val form = db.Util.createApplicationForm().copy(description = Some(description))
      val app = db.Util.createApplication(form = form)
      search.indexApplication(app.guid)

      Seq(
        app.name,
        app.key,
        description
      ).foreach { query =>
        itemsDao.findAll(Authorization.All, q = Some(query)).map(_.guid) must be(Seq(app.guid))
        itemsDao.findAll(Authorization.All, q = Some(s"   $query   ")).map(_.guid) must be(Seq(app.guid))
        itemsDao.findAll(Authorization.All, q = Some(query.toUpperCase)).map(_.guid) must be(Seq(app.guid))
      }

      itemsDao.findAll(Authorization.All, q = Some(UUID.randomUUID.toString)) must be(Nil)
    }

    "on create" in {
      val app = db.Util.createApplication()

      search.indexApplication(app.guid)

      itemsDao.findAll(Authorization.All, 
        guid = Some(app.guid)
      ).map(_.label) must be(Seq(s"${app.organization.key}/${app.key}"))
    }

    "on update" in {
      val form = db.Util.createApplicationForm()
      val app = db.Util.createApplication(form = form)

      search.indexApplication(app.guid)

      val newName = app.name + "2"

      applicationsDao.update(
        updatedBy = db.Util.createdBy,
        app = app,
        form = form.copy(name = newName)
      )

      search.indexApplication(app.guid)

      val existing = applicationsDao.findByGuid(Authorization.All, app.guid).get

      itemsDao.findAll(Authorization.All, 
        guid = Some(app.guid),
        q = Some(newName)
      ).size must be(1)
    }

    "on delete" in {
      val app = db.Util.createApplication()
      search.indexApplication(app.guid)

      applicationsDao.softDelete(db.Util.createdBy, app)
      search.indexApplication(app.guid)
      itemsDao.findAll(Authorization.All, guid = Some(app.guid)) must be(Nil)
    }

  }

}
