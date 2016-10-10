package db.generators

import com.bryzek.apidoc.api.v0.models.{GeneratorService, GeneratorServiceForm}
import db.{AuditsDao, Authorization, SoftDelete}
import com.bryzek.apidoc.api.v0.models.{Error, User}
import core.Util
import javax.inject.{Inject, Singleton}
import lib.{Pager, Validation}
import anorm._
import play.api.db._
import play.api.Play.current
import java.util.UUID

@Singleton
class ServicesDao @Inject() (
  generatorsDao: GeneratorsDao
) {

  private[this] val BaseQuery = s"""
    select services.guid,
           services.uri,
           ${AuditsDao.queryCreation("services")}
      from generators.services
     where true
  """

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
          case Some(uri) => {
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

    DB.withConnection { implicit c =>
      SQL(InsertQuery).on(
        'guid -> guid,
        'uri -> form.uri.trim,
        'created_by_guid -> user.guid
      ).execute()
    }

    global.Actors.mainActor ! actors.MainActor.Messages.GeneratorServiceCreated(guid)

    findByGuid(Authorization.All, guid).getOrElse {
      sys.error("Failed to create service")
    }
  }

  private[this] def optionIfEmpty(value: String): Option[String] = {
    value.trim match {
      case "" => None
      case v => Some(v)
    }
  }

  /**
    * Also will soft delete all generators for this service
    */
  def softDelete(deletedBy: User, service: GeneratorService) {
    Pager.eachPage { offset =>
      // Note we do not include offset in the query as each iteration
      // deletes records which will then NOT show up in the next loop
      generatorsDao.findAll(
        Authorization.All,
        serviceGuid = Some(service.guid)
      )
    } { gen =>
      generatorsDao.softDelete(deletedBy, gen)
    }
    SoftDelete.delete("generators.services", deletedBy, service.guid)
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
    val sql = Seq(
      Some(BaseQuery.trim),
      authorization.generatorServicesFilter().map { v => s"and $v" },
      guid.map { v => "and services.guid = {guid}::uuid" },
      uri.map { v => "and lower(services.uri) = lower(trim({uri}))" },
      generatorKey.map { v => "and guid = (select service_guid from generators.generators where deleted_at is null and lower(key) = lower(trim({generator_key})))" },
      isDeleted.map(db.Filters.isDeleted("services", _))
    ).flatten.mkString("\n   ") + s" order by lower(services.uri) limit ${limit} offset ${offset}"

    val bind = Seq[Option[NamedParameter]](
      guid.map('guid -> _.toString),
      uri.map('uri ->_),
      generatorKey.map('generator_key -> _)
    ).flatten ++ authorization.bindVariables

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { fromRow(_) }.toSeq
    }
  }

  private[db] def fromRow(
    row: anorm.Row,
    prefix: Option[String] = None
  ) = {
    val p = prefix.map(v => v + "_").getOrElse("")
    GeneratorService(
      guid = row[UUID](s"${p}guid"),
      uri = row[String](s"${p}uri"),
      audit = AuditsDao.fromRowCreation(row)
    )
  }

}
