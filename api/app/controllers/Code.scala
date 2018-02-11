package controllers

import java.util.UUID
import javax.inject.{Inject, Singleton}

import io.apibuilder.api.v0.models.json._
import io.apibuilder.generator.v0.Client
import io.apibuilder.generator.v0.models.{Attribute, InvocationForm}
import db.generators.{GeneratorsDao, ServicesDao}
import db.{OrganizationAttributeValuesDao, VersionsDao}
import lib.{Pager, Validation}
import play.api.libs.json._
import play.api.mvc._
import _root_.util.UserAgent
import play.api.libs.ws.WSClient

import scala.concurrent.Future

@Singleton
class Code @Inject() (
  val controllerComponents: ControllerComponents,
  wSClient: WSClient,
  organizationAttributeValuesDao: OrganizationAttributeValuesDao,
  generatorsDao: GeneratorsDao,
  servicesDao: ServicesDao,
  versionsDao: VersionsDao,
  userAgent: UserAgent
) extends BaseController {

  private[this] implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  def get(
    orgKey: String,
    applicationKey: String,
    versionName: String,
    generatorKey: String
  ) = Anonymous.async { request =>
    versionsDao.findVersion(request.authorization, orgKey, applicationKey, versionName) match {
      case None => {
        Future.successful(NotFound)
      }

      case Some(version) => {
        servicesDao.findAll(request.authorization, generatorKey = Some(generatorKey)).headOption match {
          case None => {
            Future.successful(Conflict(Json.toJson(Validation.error(s"Service with generator key[$generatorKey] not found"))))
          }

          case Some(service) => {
            generatorsDao.findAll(request.authorization, key = Some(generatorKey)).headOption match {
              case None => {
                Future.successful(Conflict(Json.toJson(Validation.error(s"Generator with key[$generatorKey] not found"))))
              }
              case Some(gws) => {
                val userAgentString = userAgent.generate(
                  orgKey = orgKey,
                  applicationKey = applicationKey,
                  versionName = version.version,
                  generatorKey = generatorKey
                )

                val attributes = getAllAttributes(version.organization.guid, gws.generator.attributes)

                new Client(wSClient, service.uri).invocations.postByKey(
                  key = gws.generator.key,
                  invocationForm = InvocationForm(
                    service = version.service,
                    userAgent = Some(userAgentString),
                    attributes = attributes
                  )
                ).map { invocation =>
                  Ok(Json.toJson(io.apibuilder.api.v0.models.Code(
                    generator = gws,
                    files = invocation.files,
                    source = invocation.source
                  )))
                }.recover {
                  case r: io.apibuilder.generator.v0.errors.ErrorsResponse => {
                    Conflict(Json.toJson(Validation.errors(r.errors.map(_.message))))
                  }
                  case r: io.apibuilder.generator.v0.errors.FailedRequest => {
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
          organizationAttributeValuesDao.findAll(
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
