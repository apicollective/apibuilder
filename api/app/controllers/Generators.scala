package controllers

import java.util.UUID

import com.gilt.apidoc.models.{GeneratorOrgForm, GeneratorUpdateForm, GeneratorCreateForm, User, Generator}
import com.gilt.apidoc.models.json._
import com.gilt.apidocgenerator.Client
import db._
import lib.Validation
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future

object Generators extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def get(
    guid: Option[UUID] = None,
    key: Option[String] = None,
    limit: Int = 100,
    offset: Int = 0
  ) = AnonymousRequest.async { request =>
    fillInGeneratorMeta(
      GeneratorDao.findAll(
        user = request.user,
        guid = guid,
        key = key,
        limit = limit,
        offset = offset
      )
    ).map(generators => Ok(Json.toJson(generators)))
  }


  def getByKey(key: String) = AnonymousRequest.async { request =>
    GeneratorDao.findAll(user = request.user, key = Some(key)).headOption match {
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

  def post() = Authenticated.async(parse.json) { request =>
    request.body.validate[GeneratorCreateForm] match {
      case e: JsError => {
        Future.successful(Conflict(Json.toJson(Validation.invalidJson(e))))
      }
      case s: JsSuccess[GeneratorCreateForm] => {
        val form = s.get
        GeneratorDao.validate(form) match {
          case Nil => {
            GeneratorDao.findAll(user = Some(request.user), keyAndUri = Some(form.key -> form.uri)).headOption match {
              case Some(d) =>
                Future.successful(Conflict(Json.toJson(Validation.error(s"generator ${form.key} already exists"))))
              case None =>
                new Client(form.uri).generators.getByKey(form.key).recover {
                  case ex: com.gilt.apidoc.FailedRequest => None
                }.map {
                  case Some(meta) =>
                    val generator = GeneratorDao.create(request.user, form.key, form.uri, form.visibility, meta.name, meta.description, meta.language)
                    Ok(Json.toJson(generator))
                  case None =>
                    NotFound("Generator uri invalid")
                }
            }
          }
          case errors => Future.successful(Conflict(Json.toJson(errors)))
        }
      }
    }
  }

  def putByKey(key: String) = Authenticated.async(parse.json) { request =>
    request.body.validate[GeneratorUpdateForm] match {
      case e: JsError => {
        Future.successful(Conflict(Json.toJson(Validation.invalidJson(e))))
      }
      case s: JsSuccess[GeneratorUpdateForm] => {
        val form = s.get
        GeneratorDao.findAll(user = Some(request.user), key = Some(key)).headOption match {
          case Some(g) =>
            fillInGeneratorMeta(g).map {
              case Right(g) if (form.visibility.isDefined && !GeneratorDao.isOwner(request.user.guid, g.owner)) =>
                Unauthorized
              case Right(g) =>
                val g1 = form.visibility.fold(g)(GeneratorDao.visibilityUpdate(request.user, g, _))
                val g2 = form.enabled.fold(g1)(GeneratorDao.userEnabledUpdate(request.user, g1, _))
                Ok(Json.toJson(g2))
              case Left(s) =>
                s
            }
          case _ =>
            Future.successful(NotFound)
        }
      }
    }
  }

  def putByKeyAndOrg(key: String, orgKey: String) = Authenticated.async(parse.json) { request =>
    request.body.validate[GeneratorOrgForm] match {
      case e: JsError => {
        Future.successful(Conflict(Json.toJson(Validation.invalidJson(e))))
      }
      case s: JsSuccess[GeneratorOrgForm] => {
        val form = s.get
        val generator = GeneratorDao.findAll(user = Some(request.user), key = Some(key)).headOption
        val org = OrganizationDao.findAll(Authorization(Some(request.user)), key = Some(orgKey)).headOption
        (generator, org) match {
          case (Some(g), Some(o)) =>
            fillInGeneratorMeta(g).map {
              case Right(g) =>
                GeneratorDao.orgEnabledUpdate(request.user, g.guid, o.guid, form.enabled)
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

  def deleteByKey(key: String) = Authenticated { request =>
    GeneratorDao.findAll(user = Some(request.user), key = Some(key)).headOption match {
      case Some(g) if GeneratorDao.isOwner(request.user.guid, g.owner) =>
        GeneratorDao.softDelete(request.user, g)
        NoContent
      case Some(g) =>
        Unauthorized
      case _ =>
        NotFound
    }
  }
}
