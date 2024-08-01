package db

import cats.implicits._
import cats.data.ValidatedNec
import cats.data.Validated.{Invalid, Valid}
import db.generated.AttributesDao
import io.apibuilder.api.v0.models.{Attribute, AttributeForm, User}
import io.flow.postgresql.Query
import lib.{UrlKey, Validation}
import anorm._
import io.apibuilder.common.v0.models.{Audit, ReferenceGuid}

import javax.inject.{Inject, Singleton}
import play.api.db._

import java.util.UUID

case class InternalAttribute(db: generated.Attribute) {
  val guid: UUID = db.guid
  val name: String = db.name
  val description: Option[String] = db.description
}

case class ValidatedAttributeForm(
  name: String,
  description: Option[String] = None
)

class InternalAttributesDao @Inject()(
  dao: AttributesDao
) {

  private def validate(
    form: AttributeForm
  ): ValidatedNec[io.apibuilder.api.v0.models.Error, ValidatedAttributeForm] = {
    (
      validateName(form.name),
      validateDescription(form.description),
    ).mapN { case (n,d) => ValidatedAttributeForm(n, d) }
  }

  private def validateName(name: String): ValidatedNec[io.apibuilder.api.v0.models.Error, String] = {
    val trimmed = name.trim
    if (trimmed.isEmpty) {
      Validation.singleError("Attribute name is required").invalidNec
    } else {
      UrlKey.validateNec(trimmed, "Name") match {
        case Invalid(e) => Validation.singleError(e.toNonEmptyList.toList.mkString(", "))
        case Valid(_) => {
          findByName(trimmed) match {
            case None => trimmed.validNec
            case Some(_) => Validation.singleError("Attribute with this name already exists").invalidNec
          }
        }
      }
    }
  }

  private def validateDescription(desc: Option[String]): ValidatedNec[io.apibuilder.api.v0.models.Error, Option[String]] = {
    desc.map(_.trim).filterNot(_.isEmpty).validNec
  }

  def create(user: User, form: AttributeForm): ValidatedNec[io.apibuilder.api.v0.models.Error, InternalAttribute] = {
    validate(form).map { vForm =>
      val guid = dao.insert(user.guid, db.generated.AttributeForm(
        name = vForm.name,
        description = vForm.description
      ))

      findByGuid(guid).getOrElse {
        sys.error("Failed to create attribute")
      }
    }
  }

  def softDelete(deletedBy: User, attributes: InternalAttribute): Unit = {
    dao.delete(deletedBy.guid, attributes.db)
  }

  def findByGuid(guid: UUID): Option[InternalAttribute] = {
    findAll(guid = Some(guid), limit = Some(1)).headOption
  }

  def findByName(name: String): Option[InternalAttribute] = {
    findAll(name = Some(name), limit = Some(1)).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    name: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Option[Long],
    offset: Long = 0
  ): Seq[InternalAttribute] = {
    dao.findAll(
      guid = guid,
      limit = limit,
      offset = offset
    ) { q =>
      q.and(name.map { _ =>
        "lower(trim(attributes.name)) = lower(trim({name}))"
      }).bind("name", name)
      .and(isDeleted.map(Filters.isDeleted("attributes", _)))
    }.map(InternalAttribute(_))
  }

}
