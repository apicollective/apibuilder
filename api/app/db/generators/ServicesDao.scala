package db.generators

import anorm._
import core.Util
import db._
import io.apibuilder.api.v0.models.{Error, GeneratorService, GeneratorServiceForm, User}
import io.apibuilder.common.v0.models.{Audit, ReferenceGuid}
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
  private val dbHelpers = DbHelpers(db, "generators.services")

  private val BaseQuery = Query(s"""
    select guid, uri,
           ${AuditsDao.queryCreationDefaultingUpdatedAt("services")}
      from generators.services
  """).withDebugging()

  private val InsertQuery = """
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
          uri = Some(form.uri.trim),
          limit = Some(1),
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
    db.withTransaction { c =>
      generatorsDao.softDeleteAllByServiceGuid(c, deletedBy, service.guid)
      dbHelpers.delete(c, deletedBy, service.guid)
    }
  }

  def findByGuid(authorization: Authorization, guid: UUID): Option[GeneratorService] = {
    findAll(authorization, guid = Some(guid), limit = Some(1)).headOption
  }

  def findAll(
    authorization: Authorization,
    guid: Option[UUID] = None,
    guids: Option[Seq[UUID]] = None,
    uri: Option[String] = None,
    generatorKey: Option[String] = None,
    isDeleted: Option[Boolean] = Some(false),
    limit: Option[Long],
    offset: Long = 0
  ): Seq[GeneratorService] = {
    db.withConnection { implicit c =>
      BaseQuery.
        equals("guid", guid).
        optionalIn("guid", guids).
        and(
          uri.map { _ =>
            "lower(uri) = lower(trim({uri}))"
          }
        ).bind("uri", uri).
        and(
          generatorKey.map { _ =>
            "guid = (select service_guid from generators.generators where deleted_at is null and lower(key) = lower(trim({generator_key})))"
          }
        ).bind("generator_key", generatorKey).
        and(isDeleted.map(Filters.isDeleted("services", _))).
        orderBy("lower(uri)").
        optionalLimit(limit).
        offset(offset).
        as(parser.*)
    }
  }

  private val parser: RowParser[GeneratorService] = {
    import org.joda.time.DateTime

    SqlParser.get[UUID]("guid") ~
      SqlParser.str("uri") ~
      SqlParser.get[DateTime]("created_at") ~
      SqlParser.get[UUID]("created_by_guid") ~
      SqlParser.get[DateTime]("updated_at") ~
      SqlParser.get[UUID]("updated_by_guid") map {
      case guid ~ uri ~ createdAt ~ createdByGuid ~ updatedAt ~ updatedByGuid => {
        GeneratorService(
          guid = guid,
          uri = uri,
          audit = Audit(
            createdAt = createdAt,
            createdBy = ReferenceGuid(createdByGuid),
            updatedAt = updatedAt,
            updatedBy = ReferenceGuid(updatedByGuid),
          )
        )
      }
    }
  }

}
