package db

import com.gilt.apidoc.api.v0.models.{Application, Organization, Version}
import anorm._
import play.api.db._
import play.api.Play.current
import java.util.UUID

object SearchVersionsDao {

  private val InsertQuery = """
    insert into search.versions
    (guid, text)
    values
    ({guid}::uuid, {text})
  """

  private val DeleteQuery = """
    delete from search.versions where guid = {guid}::uuid
  """

  def upsert(versionGuid: UUID) {
    DB.withTransaction { implicit c =>
      SQL(DeleteQuery).on(
	'guid -> versionGuid
      ).execute()

      index(versionGuid).map { text =>
        SQL(InsertQuery).on(
          'guid -> versionGuid,
          'text -> text
        ).execute()
      }
    }
  }

  private def index(versionGuid: UUID): Option[String] = {
    getInfo(versionGuid).map {
      case (org, app, version) => {
        s"""${app.name} ${app.key} ${version.version}""".trim.toLowerCase
      }
    }
  }

  private def getInfo(versionGuid: UUID): Option[(Organization, Application, Version)] = {
    VersionsDao.findByGuid(Authorization.All, versionGuid).flatMap { version =>
      ApplicationsDao.findAll(Authorization.All, version = Some(version), limit = 1).headOption.flatMap { application =>
        OrganizationsDao.findAll(Authorization.All, application = Some(application), limit = 1).headOption.map { org =>
          (org, application, version)
        }
      }
    }
  }

}