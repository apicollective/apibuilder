package controllers

import _root_.util.{ApiBuilderServiceImportResolver, UserAgent}
import db.InternalVersionsDao
import db.generated.{GeneratorInvocationForm, GeneratorInvocationsDao}
import db.generators.{GeneratorsDao, InternalGeneratorServicesDao}
import io.apibuilder.api.v0.models.json.*
import io.apibuilder.api.v0.models.{CodeForm, Version}
import io.apibuilder.generator.v0.Client
import io.apibuilder.generator.v0.models.InvocationForm
import io.apibuilder.generator.v0.models.json.*
import io.apibuilder.spec.v0.models.Service
import lib.{Constants, Validation}
import models.{GeneratorServicesModel, GeneratorWithServiceModel, VersionsModel}
import play.api.libs.json.*
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, AnyContent, Result}

import javax.inject.{Inject, Singleton}
import scala.annotation.nowarn
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Code @Inject() (
                       val apiBuilderControllerComponents: ApiBuilderControllerComponents,
                       wSClient: WSClient,
                       apiBuilderServiceImportResolver: ApiBuilderServiceImportResolver,
                       generatorInvocationsDao: GeneratorInvocationsDao,
                       generatorsDao: GeneratorsDao,
                       servicesDao: InternalGeneratorServicesDao,
                       versionsDao: InternalVersionsDao,
                       versionsModel: VersionsModel,
                       userAgentGenerator: UserAgent,
                       model: GeneratorWithServiceModel
) extends ApiBuilderController {

  case class CodeParams(
    orgKey: String,
    applicationKey: String,
    versionName: String,
    form: CodeForm,
    generatorKey: Option[String]
  ) {
    val userAgent: String = userAgentGenerator.generate(
      orgKey = orgKey,
      applicationKey = applicationKey,
      versionName = versionName,
      generatorKey = generatorKey
    )
  }


  case class InvocationFormData(
    params: CodeParams,
    userAgent: String,
    version: Version,
    importedServices: Seq[Service],
  ) {
    val invocationForm: InvocationForm = {
      InvocationForm(
        service = version.service,
        attributes = params.form.attributes,
        userAgent = Some(userAgent),
        importedServices = Some(importedServices)
      )
    }
  }

  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def getByGeneratorKey(
    orgKey: String,
    applicationKey: String,
    versionName: String,
    generatorKey: String
  ): Action[AnyContent] = Anonymous.async { request =>
    val params = CodeParams(
      orgKey = orgKey,
      applicationKey = applicationKey,
      versionName = versionName,
      form = CodeForm(attributes = Nil),
      generatorKey = Some(generatorKey)
    )
    invocationForm(request, params) match {
      case Left(errors) => Future.successful(conflict(errors))
      case Right(data) => _invoke(request, params, data, generatorKey)
    }
  }

  def postByGeneratorKey(
    orgKey: String,
    applicationKey: String,
    versionName: String,
    generatorKey: String
  ): Action[JsValue] = Anonymous.async(parse.json) { request =>
    withCodeForm(request.body) { form =>
      val params = CodeParams(
        orgKey = orgKey,
        applicationKey = applicationKey,
        versionName = versionName,
        form = form,
        generatorKey = Some(generatorKey)
      )
      invocationForm(request, params) match {
        case Left(errors) => Future.successful(conflict(errors))
        case Right(data) => _invoke(request, params, data, generatorKey)
      }
    }
  }

  def postForm(
    orgKey: String,
    applicationKey: String,
    versionName: String
  ): Action[JsValue] = Anonymous.async(parse.json) { request =>
    withCodeForm(request.body) { form =>
      invocationForm(
        request,
        CodeParams(
          orgKey = orgKey,
          applicationKey = applicationKey,
          versionName = versionName,
          generatorKey = None,
          form = form
        )
      ) match {
        case Left(errors) => Future.successful(Conflict(Json.toJson(Validation.errors(errors))))
        case Right(data) => Future.successful(Ok(Json.toJson(data.invocationForm)))
      }
    }
  }

  @nowarn
  private def _invoke(
    request: AnonymousRequest[?],
    params: CodeParams,
    data: InvocationFormData,
    generatorKey: String
  ): Future[Result] = {
    servicesDao.findAll(generatorKey = Some(generatorKey), limit = Some(1)).headOption match {
      case None => {
        Future.successful(conflict(s"Service with generator key[$generatorKey] not found"))
      }

      case Some(service) => {
        generatorsDao.findAll(request.authorization, key = Some(generatorKey))
          .flatMap(model.toModel)
          .headOption match {
          case None => {
            Future.successful(conflict(s"Generator with key[$generatorKey] not found"))
          }
          case Some(gws) => {
            // TODO: Should we merge the org attributes into the provided invocation form? I think that
            // was the original intent, but not sure if it would impact anybody. For now going to log
            // the instance for which this results in a change.
            /*
            val updatedAttributes = orgAttributeUtil.merge(data.version.organization, gws.generator.attributes, data.invocationForm.attributes)
            if (updatedAttributes != data.invocationForm.attributes) {
              val newAttributes = updatedAttributes.filterNot { a =>
                data.invocationForm.attributes.map(_.name).contains(a.name)
              }.mkString(", ")
              println(s"Code.orgAttributes org[${data.version.organization.key}] newAttributes: $newAttributes")
            } else {
              println(s"Code.orgAttributes org[${data.version.organization.key}] newAttributes: NONE")
            }
            */

            recordInvocation(params, gws.generator.key)

            new Client(wSClient, service.uri).invocations.postByKey(
              key = gws.generator.key,
              invocationForm = data.invocationForm,
            ).map { invocation =>
              Ok(Json.toJson(io.apibuilder.api.v0.models.Code(
                generator = gws,
                files = invocation.files,
                source = invocation.source
              )))
            }.recover {
              case r: io.apibuilder.generator.v0.errors.ErrorsResponse => {
                conflict(r.errors.map(_.message))
              }
              case r: io.apibuilder.generator.v0.errors.FailedRequest => {
                conflict(s"Generator failed with ${r.getMessage}")
              }
            }
          }
        }
      }
    }
  }

  private def invocationForm[T](
    request: AnonymousRequest[?],
    params: CodeParams
  ): Either[Seq[String], InvocationFormData] = {
    versionsDao.findVersion(request.authorization, params.orgKey, params.applicationKey, params.versionName)
      .flatMap(versionsModel.toModel) match {
      case None => {
        Left(Seq(s"Version [${params.versionName}] for application [${params.applicationKey}] not found"))
      }

      case Some(version) => {
        val importedServices = apiBuilderServiceImportResolver.resolve(request.authorization, version.service)
        Right(
          InvocationFormData(
            params = params,
            version = version,
            userAgent = params.userAgent,
            importedServices = importedServices
          )
        )
      }
    }
  }

  private def withCodeForm(body: JsValue)(f: CodeForm => Future[Result]) = {
    body.validate[CodeForm] match {
      case e: JsError => Future.successful(Conflict(Json.toJson(Validation.invalidJson(e))))
      case s: JsSuccess[CodeForm] => f(s.get)
    }
  }

  private def conflict(message: String): Result = {
    conflict(Seq(message))
  }

  private def conflict(messages: Seq[String]): Result = {
    Conflict(Json.toJson(Validation.errors(messages)))
  }

  private def recordInvocation(params: CodeParams, generatorKey: String): Unit = {
    generatorInvocationsDao.insert(Constants.DefaultUserGuid, GeneratorInvocationForm(
      key = generatorKey,
      organizationKey = Some(params.orgKey),
      applicationKey = Some(params.applicationKey)
    ))
    ()
  }
}
