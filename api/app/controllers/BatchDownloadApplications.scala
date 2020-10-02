package controllers

import io.apibuilder.api.v0.models.BatchDownloadApplicationsForm
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import services.BatchDownloadApplicationsService

@Singleton
class BatchDownloadApplications @Inject() (
  override val apibuilderControllerComponents: ApibuilderControllerComponents,
  service: BatchDownloadApplicationsService,
) extends ApibuilderController {

  def postApplications(orgKey: String) = Anonymous(parse.json[BatchDownloadApplicationsForm]) { request =>
    service.validate(request.authorization, orgKey, request.body)
    val versions = applicationsDao.findByOrganizationKeyAndApplicationKey(request.authorization, orgKey, applicationKey).map { application =>
      versionsDao.findAll(
        request.authorization,
        applicationGuid = Some(application.guid),
        limit = limit,
        offset = offset
      )
    }.getOrElse(Nil)
    Ok(Json.toJson(versions))
  }

}
