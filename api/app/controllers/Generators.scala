package controllers

import java.util.UUID

import com.gilt.apidoc.api.v0.errors.UnitResponse
import com.gilt.apidoc.api.v0.models.{GeneratorOrgForm, GeneratorUpdateForm, GeneratorCreateForm, User, Generator}
import com.gilt.apidoc.api.v0.models.json._
import com.gilt.apidoc.generator.v0.Client
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
    limit: Long = 100,
    offset: Long = 0
  ) = AnonymousRequest.async { request =>
    val generators = GeneratorsDao.findAll(
      request.authorization,
      guid = guid,
      key = key,
      limit = limit,
      offset = offset
    )

    fillInGeneratorMeta(
      GeneratorsDao.findAll(
        request.authorization,
        guid = guid,
        key = key,
        limit = limit,
        offset = offset
      )
    ).map(generators => Ok(Json.toJson(generators)))
  }


  def getByKey(key: String) = AnonymousRequest.async { request =>
    GeneratorsDao.findAll(request.authorization, key = Some(key)).headOption match {
      case Some(g) =>
        fillInGeneratorMeta(g).map {
          case Right(g) => Ok(Json.toJson(g))
          case Left(s) => s
        }
      case _ =>
        Future.successful(NotFound)
    }
  }

  private[this] def fillInGeneratorMeta(generator: Generator): Future[Either[Status, Generator]] = {
    new Client(generator.uri).generators.getByKey(generator.key).map { meta =>
      Right(generator.copy(name = meta.name, description = meta.description, language = meta.language))
    }.recover {
      case ex: Exception => Left(ServiceUnavailable)
    }
  }

  private[this] def fillInGeneratorMeta(generators: Seq[Generator]): Future[Seq[Generator]] = {
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
        GeneratorsDao.validate(form) match {
          case Nil => {
            GeneratorsDao.findAll(request.authorization, key = Some(form.key)).headOption match {
              case Some(d) =>
                Future.successful(Conflict(Json.toJson(Validation.error(s"A generator with the key ${form.key} already exists"))))
              case None =>
                new Client(form.uri).generators.getByKey(form.key).map { meta =>
                  val generator = GeneratorsDao.create(request.user, form.key, form.uri, form.visibility, meta.name, meta.description, meta.language)
                  Ok(Json.toJson(generator))
                }.recover {
                  case UnitResponse(404) => NotFound
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
        GeneratorsDao.findAll(request.authorization, key = Some(key)).headOption match {
          case Some(g) =>
            fillInGeneratorMeta(g).map {
              case Right(g) =>
                val g1 = form.visibility.fold(g)(GeneratorsDao.visibilityUpdate(request.user, g, _))
                Ok(Json.toJson(g1))
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
        val generator = GeneratorsDao.findAll(request.authorization, key = Some(key)).headOption
         val org = OrganizationsDao.findAll(request.authorization, key = Some(orgKey)).headOption
         (generator, org) match {
           case (Some(g), Some(o)) =>
             fillInGeneratorMeta(g).map {
               case Right(g) =>
                 GeneratorsDao.orgEnabledUpdate(request.user, g.guid, o.guid, form.enabled)
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
     GeneratorsDao.findAll(request.authorization, key = Some(key)).headOption match {
      case Some(g) => {
        GeneratorsDao.softDelete(request.user, g)
        NoContent
      }
      case None => NotFound
    }
  }
}
