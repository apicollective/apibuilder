package controllers

import java.util.UUID

import com.gilt.apidoc.api.v0.errors.FailedRequest
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
    println("limit: " + limit)
    val generators = GeneratorsDao.findAll(
      Authorization(request.user),
      guid = guid,
      key = key,
      limit = limit,
      offset = offset
    )
    println("guid:" + guid)
    println("key:" + key)
    println("generators: " + generators.size)

    fillInGeneratorMeta(
      GeneratorsDao.findAll(
        Authorization(request.user),
        guid = guid,
        key = key,
        limit = limit,
        offset = offset
      )
    ).map(generators => Ok(Json.toJson(generators)))
  }


  def getByKey(key: String) = AnonymousRequest.async { request =>
    GeneratorsDao.findAll(Authorization(request.user), key = Some(key)).headOption match {
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
    println(s"fillInGeneratorMeta(${generator.key})")
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
        GeneratorsDao.validate(form) match {
          case Nil => {
            GeneratorsDao.findAll(Authorization.User(request.user.guid), keyAndUri = Some(form.key -> form.uri)).headOption match {
              case Some(d) =>
                Future.successful(Conflict(Json.toJson(Validation.error(s"generator ${form.key} already exists"))))
              case None =>
                new Client(form.uri).generators.getByKey(form.key).recover {
                  case ex: FailedRequest => None
                }.map {
                  case Some(meta) =>
                    val generator = GeneratorsDao.create(request.user, form.key, form.uri, form.visibility, meta.name, meta.description, meta.language)
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
        GeneratorsDao.findAll(Authorization.User(request.user.guid), key = Some(key)).headOption match {
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
        val generator = GeneratorsDao.findAll(Authorization.User(request.user.guid), key = Some(key)).headOption
         val org = OrganizationsDao.findAll(Authorization(Some(request.user)), key = Some(orgKey)).headOption
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
     GeneratorsDao.findAll(Authorization.User(request.user.guid), key = Some(key)).headOption match {
      case Some(g) => {
        GeneratorsDao.softDelete(request.user, g)
        NoContent
      }
      case None => NotFound
    }
  }
}
