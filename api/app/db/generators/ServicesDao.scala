package db.generators

import anorm._
import core.Util
import db._
import io.apibuilder.api.v0.models.{Error, GeneratorService, GeneratorServiceForm, User}
import io.apibuilder.task.v0.models.TaskType
import io.flow.postgresql.Query
import lib.{Pager, Validation}
import play.api.db._

import java.util.UUID
import javax.inject.{Inject, Singleton}

@Singleton
class ServicesDao @Inject() (
  @NamedDatabase("default") db: Database,
  generatorsDao: GeneratorsDao,
  internalTasksDao: InternalTasksDao
) {
  private[this] val dbHelpers = DbHelpers(db, "generators.services")

  private[this] val BaseQuery = Query(s"""
    select services.guid,
           services.uri,
           ${AuditsDao.queryCreationDefaultingUpdatedAt("services")}
      from generators.services
  """)

  private[this] val InsertQuery = """
    insert into generators.services
    (guid, uri, created_by_guid)
    values
    ({guid}::uuid, {uri}, {created_by_guid}::uuid)
  """

  def validate(
    form: GeneratorServiceForm
  ): Seq[Error] = {
    val uriErrors = Util.validateUri(form.uri.trim) match {
      case Nil => {
        findAll(
          Authorization.All,
          uri = Some(form.uri.trim)
        ).headOption match {
          case None => Nil
          case Some(_) => {
            Seq(s"URI[${form.uri.trim}] already exists")
          }
        }
      }
      case errors => errors
    }

    Validation.errors(uriErrors)
  }

  def create(user: User, form: GeneratorServiceForm): GeneratorService = {
    val errors = validate(form)
    assert(errors.isEmpty, errors.map(_.message).mkString("\n"))

    val guid = UUID.randomUUID

    db.withTransaction { implicit c =>
      SQL(InsertQuery).on(
        "guid" -> guid,
        "uri" -> form.uri.trim,
        "created_by_guid" -> user.guid
      ).execute()
      internalTasksDao.queueWithConnection(c, TaskType.SyncGeneratorService, guid.toString)
    }

    findByGuid(Authorization.All, guid).getOrElse {
      sys.error("Failed to create service")
    }
  }

  /**
    * Also will soft delete all generators for this service
    */
  def softDelete(deletedBy: User, service: GeneratorService): Unit = {
    Pager.eachPage { _ =>
      // Note we do not include offset in the query as each iteration
      // deletes records which will then NOT show up in the next loop
      generatorsDao.findAll(
        Authorization.All,
        serviceGuid = Some(service.guid)
      )
    } { gen =>
      generatorsDao.softDelete(deletedBy, gen)
    }
    dbHelpers.delete(deletedBy, service.guid)
  }

  def findByGuid(authorization: Authorization, guid: UUID): Option[GeneratorService] = {
    findAll(authorization, guid = Some(guid)).headOption
  }

  def findAll(
    authorization: Authorization,
    guid: Option[UUID] = None,
    uri: Option[String] = None,
    generatorKey: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Long = 25,
    offset: Long = 0
  ): Seq[GeneratorService] = {
    db.withConnection { implicit c =>
      authorization.generatorServicesFilter(BaseQuery).
        equals("services.guid", guid).
        and(
          uri.map { _ =>
            "lower(services.uri) = lower(trim({uri}))"
          }
        ).bind("uri", uri).
        and(
          generatorKey.map { _ =>
            "services.guid = (select service_guid from generators.generators where deleted_at is null and lower(key) = lower(trim({generator_key})))"
          }
        ).bind("generator_key", generatorKey).
        and(isDeleted.map(Filters.isDeleted("services", _))).
        orderBy("lower(services.uri)").
        limit(limit).
        offset(offset).
        as(io.apibuilder.api.v0.anorm.parsers.GeneratorService.parser().*)
    }
  }

}
