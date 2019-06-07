package controllers

import java.util.UUID

import javax.inject.{Inject, Singleton}
import io.apibuilder.api.v0.models.json._
import io.apibuilder.api.v0.models.{CodeForm, GeneratorService, GeneratorWithService, Version}
import io.apibuilder.generator.v0.Client
import io.apibuilder.generator.v0.models.{Attribute, InvocationForm}
import io.apibuilder.generator.v0.models.json._
import db.generators.{GeneratorsDao, ServicesDao}
import db.{OrganizationAttributeValuesDao, VersionsDao}
import lib.{Pager, Validation}
import play.api.libs.json._
import _root_.util.UserAgent
import _root_.util.ApibuilderServiceImportResolver
import io.apibuilder.spec.v0.models.Service
import play.api.libs.ws.WSClient
import play.api.mvc.Result

import scala.concurrent.Future

case class InvocationFormData(
  params: CodeParams,
  version: Version,
  service: GeneratorService,
  gws: GeneratorWithService,
  importedServices: Seq[Service]
) {
  val invocationForm: InvocationForm = {
    InvocationForm(
      service = version.service,
      attributes = params.form.attributes,
      importedServices = Some(importedServices)
    )
  }
}

case class CodeParams(
  orgKey: String,
  applicationKey: String,
  versionName: String,
  generatorKey: String,
  form: CodeForm
)

@Singleton
class Code @Inject() (
  val apibuilderControllerComponents: ApibuilderControllerComponents,
  wSClient: WSClient,
  organizationAttributeValuesDao: OrganizationAttributeValuesDao,
  generatorsDao: GeneratorsDao,
  servicesDao: ServicesDao,
  versionsDao: VersionsDao,
  userAgent: UserAgent
) extends ApibuilderController {

  private[this] implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
  private[this] def userAgent(params: CodeParams, versionName: String): String = {
    userAgent.generate(
      orgKey = params.orgKey,
      applicationKey = params.applicationKey,
      versionName = versionName,
      generatorKey = params.generatorKey
    )
  }

  def get(
    orgKey: String,
    applicationKey: String,
    versionName: String,
    generatorKey: String
  ) = Anonymous.async { request =>
    val params = CodeParams(
      orgKey = orgKey,
      applicationKey = applicationKey,
      versionName = versionName,
      generatorKey = generatorKey,
      form = CodeForm(attributes = Nil)
    )
    invocationForm(request, params) match {
      case Left(errors) => Future.successful(Conflict(Json.toJson(Validation.errors(errors))))
      case Right(data) => _invoke(request, params, data)
    }
  }

  def post(
    orgKey: String,
    applicationKey: String,
    versionName: String,
    generatorKey: String
  ) = Anonymous.async(parse.json) { request =>
    withCodeForm(request.body) { form =>
      val params = CodeParams(
        orgKey = orgKey,
        applicationKey = applicationKey,
        versionName = versionName,
        generatorKey = generatorKey,
        form = form
      )
      invocationForm(request, params) match {
        case Left(errors) => Future.successful(Conflict(Json.toJson(Validation.errors(errors))))
        case Right(data) => _invoke(request, params, data)
      }
    }
  }

  def postForm(
    orgKey: String,
    applicationKey: String,
    versionName: String,
    generatorKey: String
  ) = Anonymous.async(parse.json) { request =>
    withCodeForm(request.body) { form =>
      invocationForm(
        request,
        CodeParams(
          orgKey = orgKey,
          applicationKey = applicationKey,
          versionName = versionName,
          generatorKey = generatorKey,
          form = form
        )
      ) match {
        case Left(errors) => Future.successful(Conflict(Json.toJson(Validation.errors(errors))))
        case Right(data) => Future.successful(Ok(Json.toJson(data.invocationForm)))
      }
    }
  }

  private def _invoke(
    request: AnonymousRequest[_],
    params: CodeParams,
    data: InvocationFormData
  ) = {
    val orgAttributes = getAllAttributes(data.version.organization.guid, data.gws.generator.attributes)

    new Client(wSClient, data.service.uri).invocations.postByKey(
      key = data.gws.generator.key,
      invocationForm = InvocationForm(
        service = data.version.service,
        userAgent = Some(userAgent(params, data.version.version)),
        attributes = data.params.form.attributes ++ orgAttributes,
        importedServices = Some(data.importedServices)
      )
    ).map { invocation =>
      Ok(Json.toJson(io.apibuilder.api.v0.models.Code(
        generator = data.gws,
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


  private def invocationForm[T](
    request: AnonymousRequest[_],
    params: CodeParams
  ): Either[Seq[String], InvocationFormData] = {
    versionsDao.findVersion(request.authorization, params.orgKey, params.applicationKey, params.versionName) match {
      case None => {
        Left(Seq(s"Version [${params.versionName}] for application [${params.applicationKey}] not found"))
      }

      case Some(version) => {
        servicesDao.findAll(request.authorization, generatorKey = Some(params.generatorKey)).headOption match {
          case None => {
            Left(Seq(s"Service with generator key[${params.generatorKey}] not found"))
          }

          case Some(service) => {
            generatorsDao.findAll(request.authorization, key = Some(params.generatorKey)).headOption match {
              case None => {
                Left(Seq(s"Generator with key[${params.generatorKey}] not found"))
              }
              case Some(gws) => {
                val importedApibuilderServices = ApibuilderServiceImportResolver
                  .resolveChildren(version.service, versionsDao, request.authorization).values.toSeq
                Right(
                  InvocationFormData(
                    params = params,
                    version = version,
                    service = service,
                    gws = gws,
                    importedServices = importedApibuilderServices
                  )
                )
              }
            }
          }
        }
      }
    }
  }

  private[this] def withCodeForm(body: JsValue)(f: CodeForm => Future[Result]) = {
    body.validate[CodeForm] match {
      case e: JsError => Future.successful(Conflict(Json.toJson(Validation.invalidJson(e))))
      case s: JsSuccess[CodeForm] => f(s.get)
    }
  }
}
