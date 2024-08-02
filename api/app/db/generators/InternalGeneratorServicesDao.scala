package db.generators

import cats.data.ValidatedNec
import cats.implicits.*
import cats.data.Validated.{Invalid, Valid}
import core.Util
import db.*
import db.generated.generators.ServicesDao
import io.apibuilder.api.v0.models.{Error, GeneratorService, GeneratorServiceForm, User}
import io.apibuilder.common.v0.models.{Audit, ReferenceGuid}
import io.apibuilder.task.v0.models.TaskType
import io.flow.postgresql.{OrderBy, Query}
import lib.Validation
import play.api.db.*

import java.util.UUID
import javax.inject.{Inject, Singleton}

case class InternalGeneratorService(db: generated.generators.Service) {
  val guid: UUID = db.guid
  val uri: String = db.uri
}

case class ValidatedGeneratorServiceForm(uri: String)

class InternalGeneratorServicesDao @Inject()(
                                              dao: ServicesDao,
                                              generatorsDao: InternalGeneratorsDao,
                                              internalTasksDao: InternalTasksDao
) {

  def validate(
    form: GeneratorServiceForm
  ): ValidatedNec[Error, ValidatedGeneratorServiceForm] = {
    validateUri(form.uri).map { vUri =>
      ValidatedGeneratorServiceForm(uri = vUri)
    }
  }

  private def validateUri(uri: String): ValidatedNec[Error, String] = {
    Util.validateUriNec(uri) match {
      case Invalid(e) => Validation.singleError(e.toNonEmptyList.toList.mkString(", ")).invalidNec
      case Valid(url) => {
        findAll(
          uri = Some(url),
          limit = Some(1),
        ).headOption match {
          case None => url.validNec
          case Some(_) => Validation.singleError(s"URI[$url] already exists").invalidNec
        }
      }
    }
  }

  def create(user: InternalUser, form: GeneratorServiceForm): ValidatedNec[Error, InternalGeneratorService] = {
    validate(form).map { vForm =>
      val guid = dao.db.withTransaction { c =>
        val guid = dao.insert(c, user.guid, generated.generators.ServiceForm(
          uri = vForm.uri
        ))
        internalTasksDao.queueWithConnection(c, TaskType.SyncGeneratorService, guid.toString)
        guid
      }

      findByGuid(guid).getOrElse {
        sys.error("Failed to create generator service")
      }
    }
  }

  /**
    * Also will soft delete all generators for this service
    */
  def softDelete(deletedBy: InternalUser, service: InternalGeneratorService): Unit = {
    dao.db.withTransaction { c =>
      generatorsDao.softDeleteAllByServiceGuid(c, deletedBy, service.guid)
      dao.delete(c, deletedBy.guid, service.db)
    }
  }

  def findByGuid(guid: UUID): Option[InternalGeneratorService] = {
    findAll(guid = Some(guid), limit = Some(1)).headOption
  }

  def findAll(
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    uri: Option[String] = None,
    generatorKey: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Option[Long],
    offset: Long = 0
  ): Seq[InternalGeneratorService] = {
    dao.findAll(
      guid = guid,
      guids = guids,
      limit = limit,
      offset = offset,
      orderBy = Some(OrderBy("lower(uri)"))
    ) { q =>
      q.and(
        uri.map { _ =>
          "lower(uri) = lower(trim({uri}))"
        }
      ).bind("uri", uri)
        .and(
          generatorKey.map { _ =>
            "guid = (select service_guid from generators.generators where deleted_at is null and lower(key) = lower(trim({generator_key})))"
          }
        ).bind("generator_key", generatorKey)
        .and(isDeleted.map(Filters.isDeleted("services", _)))
    }.map(InternalGeneratorService(_))
  }

}
