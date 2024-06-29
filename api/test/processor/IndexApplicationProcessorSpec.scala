package processor

import db.Authorization
import lib.TestHelper
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import java.util.UUID

class IndexApplicationProcessorSpec extends PlaySpec with GuiceOneAppPerSuite with db.Helpers with TestHelper {

  private def processor = injector.instanceOf[IndexApplicationProcessor]

  private def indexApplication(guid: UUID): Unit = {
    expectValid {
      processor.processRecord(guid)
    }
  }

  "indexApplication" must {

    "q" in {
      val description = UUID.randomUUID.toString
      val form = createApplicationForm().copy(description = Some(description))
      val app = createApplication(form = form)
      indexApplication(app.guid)

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
      val app = createApplication()
      indexApplication(app.guid)

      itemsDao.findAll(Authorization.All, 
        guid = Some(app.guid)
      ).map(_.label) must be(Seq(s"${app.organization.key}/${app.key}"))
    }

    "on update" in {
      val form = createApplicationForm()
      val app = createApplication(form = form)
      indexApplication(app.guid)

      val newName = app.name + "2"

      applicationsDao.update(
        updatedBy = testUser,
        app = app,
        form = form.copy(name = newName)
      )
      indexApplication(app.guid)

      val existing = applicationsDao.findByGuid(Authorization.All, app.guid).get

      itemsDao.findAll(Authorization.All, 
        guid = Some(app.guid),
        q = Some(newName)
      ).size must be(1)
    }

    "on delete" in {
      val app = createApplication()
      indexApplication(app.guid)

      applicationsDao.softDelete(testUser, app)
      indexApplication(app.guid)
      itemsDao.findAll(Authorization.All, guid = Some(app.guid)) must be(Nil)
    }

  }

}
