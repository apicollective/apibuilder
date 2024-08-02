package db

import db.generated.OriginalsDao
import io.apibuilder.api.v0.models.{Original, OriginalType}

import java.util.UUID
import javax.inject.Inject

case class InternalOriginal(db: generated.Original) {
  val id: Long = db.id
  val versionGuid: UUID = db.versionGuid
  val `type`: OriginalType = OriginalType(db.`type`)
  val data: String = db.data
}

class InternalOriginalsDao @Inject()(dao: OriginalsDao) {

  def create(
    implicit c: java.sql.Connection,
    user: InternalUser,
    versionGuid: UUID,
    original: Original
  ): Unit = {
    dao.insert(c, user.guid, generated.OriginalForm(
      versionGuid = versionGuid,
      `type` = original.`type`.toString,
      data = original.data,
    ))
  }

  def softDeleteByVersionGuid(
    implicit c: java.sql.Connection,
    user: InternalUser,
    versionGuid: UUID
  ): Unit = {
    dao.deleteAllByVersionGuid(c, user.guid, versionGuid)
  }

  def findAllByVersionGuids(versionGuids: Seq[UUID]): Seq[InternalOriginal] = {
    dao.findAll(
      versionGuids = Some(versionGuids),
      limit = None,
    ) { q => q.isNull("deleted_at") }.map(InternalOriginal(_))
  }

  def findByVersionGuid(guid: UUID): Option[InternalOriginal] = {
    findAllByVersionGuids(Seq(guid)).headOption
  }

}
