package controllers

import java.util.UUID

import com.bryzek.apidoc.api.v0.models.json._

import com.bryzek.apidoc.spec.v0.models.json._
import com.bryzek.apidoc.spec.v0.models.Service

import com.bryzek.apidoc.generator.v0.Client
import com.bryzek.apidoc.generator.v0.models.{Attribute, InvocationForm}

import db.generators.{GeneratorsDao, ServicesDao}
import db.{Authorization, OrganizationAttributeValuesDao, VersionsDao}
import lib.{Config, AppConfig, Pager, Validation}

import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future

object Code extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  private[this] val apidocVersion = Config.requiredString("git.version")

  def get(
    orgKey: String,
    applicationKey: String,
    versionName: String,
    generatorKey: String
  ) = AnonymousRequest.async { request =>
    VersionsDao.findVersion(request.authorization, orgKey, applicationKey, versionName) match {
      case None => {
        Future.successful(NotFound)
      }

      case Some(version) => {
        ServicesDao.findAll(request.authorization, generatorKey = Some(generatorKey)).headOption match {
          case None => {
            Future.successful(Conflict(Json.toJson(Validation.error(s"Service with generator key[$generatorKey] not found"))))
          }

          case Some(service) => {
            GeneratorsDao.findAll(request.authorization, key = Some(generatorKey)).headOption match {
              case None => {
                Future.successful(Conflict(Json.toJson(Validation.error(s"Generator with key[$generatorKey] not found"))))
              }
              case Some(gws) => {
                val userAgent = s"apidoc:$apidocVersion ${AppConfig.apidocWwwHost}/${orgKey}/${applicationKey}/${version.version}/${gws.generator.key}"

                val attributes = getAllAttributes(version.organization.guid, gws.generator.attributes)
                println("ATTRIBUTES: " + attributes.mkString(", "))

                new Client(service.uri).invocations.postByKey(
                  key = gws.generator.key,
                  invocationForm = InvocationForm(
                    service = version.service,
                    userAgent = Some(userAgent),
                    attributes = Some(attributes)
                  )
                ).map { invocation =>
                  Ok(Json.toJson(com.bryzek.apidoc.api.v0.models.Code(
                    generator = gws,
                    files = invocation.files,
                    source = invocation.source
                  )))
                }.recover {
                  case r: com.bryzek.apidoc.generator.v0.errors.ErrorsResponse => {
                    Conflict(Json.toJson(Validation.errors(r.errors.map(_.message))))
                  }
                  case r: com.bryzek.apidoc.generator.v0.errors.FailedRequest => {
                    Conflict(Json.toJson(Validation.errors(Seq(s"Generator failed with ${r.getMessage}"))))
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  /**
    * Fetch all attribute values specified for this organization,
    * filtered by those matching names.
    */
  private[this] def getAllAttributes(organizationGuid: UUID, names: Seq[String]): Seq[Attribute] = {
    names match {
      case Nil => Nil
      case _ => {
        var all = scala.collection.mutable.ListBuffer[Attribute]()

        Pager.eachPage { offset =>
          OrganizationAttributeValuesDao.findAll(
            organizationGuid = Some(organizationGuid),
            attributeNames = Some(names),
            offset = offset
          )
        } { av =>
          all += Attribute(av.attribute.name, av.value)
        }

        all
      }
    }
  }

}
