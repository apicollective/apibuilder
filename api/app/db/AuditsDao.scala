package db

import com.gilt.apidoc.v0.models.{Audit, ReferenceGuid}
import db.AnormHelper._
import org.joda.time.DateTime
import java.util.UUID

object AuditsDao {

  def query(tableName: String) = {
    Seq(
      queryCreation(tableName),
      s"${tableName}.updated_at",
      s"${tableName}.updated_by_guid"
    ).mkString(", ")
  }

  def queryCreation(tableName: String) = {
    Seq(
      s"${tableName}.created_at",
      s"${tableName}.created_by_guid"
    ).mkString(", ")
  }

  def queryWithAlias(tableName: String, prefix: String) = {
    Seq(
      s"${tableName}.created_at as ${prefix}_created_at",
      s"${tableName}.created_by_guid as ${prefix}_created_by_guid",
      s"${tableName}.updated_at as ${prefix}_updated_at",
      s"${tableName}.updated_by_guid as ${prefix}_updated_by_guid"
    ).mkString(", ")
  }

  private[db] def fromRow(
    row: anorm.Row,
    prefix: Option[String] = None
  ): Audit = {
    val p = prefix.map( _ + "_").getOrElse("")
    Audit(
      createdAt = row[DateTime](s"${p}created_at"),
      createdBy = ReferenceGuid(
        guid = row[UUID](s"${p}created_by_guid")
      ),
      updatedAt = row[DateTime](s"${p}updated_at"),
      updatedBy = ReferenceGuid(
        guid = row[UUID](s"${p}updated_by_guid")
      )
    )
  }

  private[db] def fromRowCreation(
    row: anorm.Row,
    prefix: Option[String] = None
  ): Audit = {
    val p = prefix.map( _ + "_").getOrElse("")

    val createdAt = row[DateTime](s"${p}created_at")
    val createdBy = ReferenceGuid(
      guid = row[UUID](s"${p}created_by_guid")
    )

    Audit(
      createdAt = createdAt,
      createdBy = createdBy,
      updatedAt = createdAt,
      updatedBy = createdBy
    )
  }

}
