package controllers

import java.util.UUID

import com.gilt.apidoc.models.{User, Generator}
import com.gilt.apidoc.models.json._
import com.gilt.apidocgenerator.Client
import db._
import lib.Validation
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future

object Generators extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def getByGuid(guid: UUID) = Authenticated.async { request =>
    GeneratorDao.findAll(user = request.user, guid = Some(guid)).headOption match {
      case Some(g) =>
        fillInGeneratorMeta(g).map {
          case Right(g) => Ok(Json.toJson(g))
          case Left(s) => s
        }
      case _ =>
        Future.successful(NotFound)
    }
  }

  private def fillInGeneratorMeta(generator: Generator): Future[Either[Status, Generator]] = {
    new Client(generator.uri).generators.getByKey(generator.key).map {
      case Some(meta) => Right(generator.copy(name = meta.name, description = meta.description, language = meta.language))
      case _ => Left(NotFound)
    }.recover {
      case ex: Exception => Left(ServiceUnavailable)
    }
  }

  private def fillInGeneratorMeta(generators: Seq[Generator]): Future[Seq[Generator]] = {
      val futures: Seq[Future[Option[Generator]]] = generators.map {
        fillInGeneratorMeta(_).map {
          case Right(g) => Some(g)
          case _ => None
        }
      }
      Future.sequence(futures).map(_.flatten)
    }

  private def getGenerators(user: User): Future[Seq[Generator]] = {
    fillInGeneratorMeta(GeneratorDao.findAll(user = user))
  }

  def get() = Authenticated.async { request =>
    getGenerators(request.user).map(generators => Ok(Json.toJson(generators)))
  }

  def post() = Authenticated.async(parse.json) { request =>
    request.body.validate[GeneratorCreateForm] match {
      case e: JsError => {
        Future.successful(Conflict(Json.toJson(Validation.error("invalid json document: " + e.toString))))
      }
      case s: JsSuccess[GeneratorCreateForm] => {
        val form = s.get
        GeneratorDao.findAll(user = request.user, keyAndUri = Some(form.key -> form.uri)).headOption match {
          case None =>
            new Client(form.uri).generators.getByKey(form.key).recover {
              case ex: Exception => None
            }.map {
              case Some(meta) =>
                val generator = GeneratorDao.create(request.user, form.key, form.uri, form.visibility, meta.name, meta.description, meta.language)
                Ok(Json.toJson(generator))
              case None =>
                NotFound("Generator uri invalid")
            }
          case Some(d) =>
            Future.successful(Conflict(Json.toJson(Validation.error("generator already exists"))))
        }
      }
    }
  }

  def putByGuid(guid: UUID) = Authenticated.async(parse.json) { request =>
    request.body.validate[GeneratorUpdateForm] match {
      case e: JsError => {
        Future.successful(Conflict(Json.toJson(Validation.error("invalid json document: " + e.toString))))
      }
      case s: JsSuccess[GeneratorUpdateForm] => {
        val form = s.get
        GeneratorDao.findAll(user = request.user, guid = Some(guid)).headOption match {
          case Some(g) =>
            fillInGeneratorMeta(g).map {
              case Right(g) =>
                val generator = GeneratorDao.update(request.user, g, form)
                Ok(Json.toJson(generator))
              case Left(s) =>
                s
            }
          case _ =>
            Future.successful(NotFound)
        }
      }
    }
  }

  def putByGuidAndOrg(guid: UUID, orgKey: String) = Authenticated.async(parse.json) { request =>
    request.body.validate[GeneratorOrgUpdateForm] match {
      case e: JsError => {
        Future.successful(Conflict(Json.toJson(Validation.error("invalid json document: " + e.toString))))
      }
      case s: JsSuccess[GeneratorOrgUpdateForm] => {
        val form = s.get
        val generator = GeneratorDao.findAll(user = request.user, guid = Some(guid)).headOption
        val org = OrganizationDao.findAll(Authorization(Some(request.user)), key = Some(orgKey)).headOption
        (generator, org) match {
          case (Some(g), Some(o)) =>
            fillInGeneratorMeta(g).map {
              case Right(g) =>
                GeneratorDao.orgUpdate(request.user, g.guid, o.guid, form.enabled)
                Ok(Json.toJson(g))
              case Left(s) =>
                s
            }
          case _ =>
            Future.successful(NotFound)
        }
      }
    }
  }

  def deleteByGuid(guid: UUID) = Authenticated { request =>
    GeneratorDao.findAll(user = request.user, guid = Some(guid)).headOption match {
      case Some(g) =>
        GeneratorDao.softDelete(request.user, g)
        NoContent
      case _ =>
        NotFound
    }
  }
}
