package models

import builder.api_json.upgrades.ServiceParser
import cats.data.Validated.{Invalid, Valid}
import cats.data.ValidatedNec
import cats.implicits._
import db.{ApplicationsDao, Authorization, InternalVersion, OrganizationsDao}
import io.apibuilder.api.v0.models.Version
import io.apibuilder.common.v0.models.Reference
import io.apibuilder.spec.v0.models.Service

import javax.inject.Inject

class VersionsModel @Inject()(
                                        applicationsDao: ApplicationsDao,
                                        organizationsDao: OrganizationsDao,
                                        serviceParser: ServiceParser,
                                        ) {
  def toModel(v: InternalVersion): Option[Version] = {
    toModels(Seq(v)).headOption
  }

  def toModels(versions: Seq[InternalVersion]): Seq[Version] = {
    validateModels(versions) match {
      case Invalid(e) => {
        println(s"Could not convert versions to models: ${e.toNonEmptyList.toList.mkString(", ")}")
        Nil
      }
      case Valid(versions) => versions
    }
  }

  private def validateModels(versions: Seq[InternalVersion]): ValidatedNec[String, Seq[Version]] = {
    val applications = applicationsDao.findAll(
      Authorization.All,
      guids = Some(versions.map(_.applicationGuid)),
      limit = None
    ).map { o => o.guid -> o }.toMap

    val organizations = organizationsDao.findAll(
      Authorization.All,
      guids = Some(applications.values.map(_.organizationGuid).toSeq),
      limit = None
    ).map { o => o.guid -> o }.toMap

    versions.map { v =>
      (
        applications.get(v.applicationGuid).toValidNec("Cannot find application").andThen { app =>
          organizations.get(app.organizationGuid).toValidNec("Cannot find organization").map { org =>
            (org, app)
          }
        },
        service(v)
      ).mapN { case ((org, app), svc) =>
        Version(
          guid = v.guid,
          organization = Reference(guid = org.guid, key = org.key),
          application = Reference(guid = app.guid, key = app.key),
          version = v.version,
          original = v.original,
          service = svc,
          audit = v.audit
        )
      }
    }.sequence
  }

  private def service(v: InternalVersion): ValidatedNec[String, Service] = {
    v.serviceJson match {
      case None => "Version does not have service json".invalidNec
      case Some(json) => serviceParser.fromString(json)
    }
  }
}