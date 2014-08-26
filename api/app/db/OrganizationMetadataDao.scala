package db

import com.gilt.apidoc.models.json._
import com.gilt.apidoc.models.{OrganizationMetadata, User, Version}
import core.Text
import lib.{Validation, ValidationError}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import java.util.UUID

case class OrganizationMetadataForm(
  package_name: Option[String] = None
)

object OrganizationMetadataForm {
  implicit val OrganizationMetadataFormReads = Json.reads[OrganizationMetadataForm]

  val Empty = OrganizationMetadataForm()

  def validate(form: OrganizationMetadataForm): Seq[ValidationError] = {
    form.package_name match {
      case None => Seq.empty
      case Some(name: String) => {
        if (isValidPackageName(name)) {
          Seq.empty
        } else {
          Validation.invalidName
        }
      }
    }
  }

  private[db] def isValidPackageName(name: String): Boolean = {
    name.split("\\.").find(!Text.isValidName(_)) match {
      case None => true
      case _ => false
    }
  }
}

object OrganizationMetadataDao {

  private val BaseQuery = """
    select guid::varchar, organization_guid::varchar, package_name
      from organization_metadata
     where deleted_at is null
       and organization_guid = {organization_guid}::uuid
  """

  private val InsertQuery = """
    insert into organization_metadata
    (guid, organization_guid, package_name, created_by_guid)
    values
    ({guid}::uuid, {organization_guid}::uuid, {package_name}, {created_by_guid}::uuid)
  """

  private val SoftDeleteQuery = """
    update organization_metadata
       set deleted_by_guid = {deleted_by_guid}::uuid,
           deleted_at = now()
     where organization_guid = {organization_guid}::uuid
       and deleted_at is null
  """


  def upsert(user: User, org: Organization, form: OrganizationMetadataForm): OrganizationMetadata = {
    DB.withTransaction { implicit c =>
      softDelete(c, user, org)
      create(c, user, org, form)
    }
  }

  private[db] def create(createdBy: User, org: Organization, form: OrganizationMetadataForm): OrganizationMetadata = {
    DB.withConnection { implicit c =>
      create(c, createdBy, org, form)
    }
  }

  private[db] def create(implicit c: java.sql.Connection, createdBy: User, org: Organization, form: OrganizationMetadataForm): OrganizationMetadata = {
    val metadata = OrganizationMetadata(
      packageName = form.package_name
    )
    val guid = UUID.randomUUID.toString

    SQL(InsertQuery).on(
      'guid -> guid,
      'organization_guid -> org.guid,
      'package_name -> metadata.packageName,
      'created_by_guid -> createdBy.guid
    ).execute()

    metadata
  }

  private[db] def softDelete(deletedBy: User, org: Organization) {
    DB.withConnection { implicit c =>
      softDelete(c, deletedBy, org)
    }
  }

  private[db] def softDelete(implicit c: java.sql.Connection, deletedBy: User, org: Organization) {
    SQL(SoftDeleteQuery).on('deleted_by_guid -> deletedBy.guid, 'organization_guid -> org.guid).execute()
  }

  def findByOrganizationGuid(organizationGuid: String): Option[OrganizationMetadata] = {
    DB.withConnection { implicit c =>
      SQL(BaseQuery).on('organization_guid -> organizationGuid)().toList.map { row =>
        OrganizationMetadata(
          packageName = row[Option[String]]("package_name")
        )
      }.toSeq.headOption
    }
  }

}
