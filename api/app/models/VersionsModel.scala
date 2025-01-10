package models

import builder.api_json.upgrades.ServiceParser
import cats.data.Validated.{Invalid, Valid}
import cats.data.ValidatedNec
import cats.implicits.*
import db.{Authorization, InternalApplicationsDao, InternalOrganizationsDao, InternalOriginalsDao, InternalVersion}
import db.generated.cache.ServicesDao
import io.apibuilder.api.v0.models.Version
import io.apibuilder.common.v0.models.{Audit, Reference, ReferenceGuid}
import io.apibuilder.spec.v0.models.{Apidoc, Service}
import io.apibuilder.spec.v0.models.json.*
import io.flow.postgresql.OrderBy

import javax.inject.Inject

class VersionsModel @Inject()(
                               applicationsDao: InternalApplicationsDao,
                               organizationsDao: InternalOrganizationsDao,
                               originalsDao: InternalOriginalsDao,
                               serviceParser: ServiceParser,
                               servicesDao: ServicesDao,
                               originalsModel: OriginalsModel
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

    val originals = originalsDao.findAllByVersionGuids(versions.map(_.guid)).map { o => o.versionGuid -> o }.toMap
    
    versions.map { v =>
      (
        applications.get(v.applicationGuid).toValidNec("Cannot find application").andThen { app =>
          organizations.get(app.organizationGuid).toValidNec("Cannot find organization").map { org =>
            (org, app)
          }
        },
        service(v),
      ).mapN { case ((org, app), svc) =>
        Version(
          guid = v.guid,
          organization = Reference(guid = org.guid, key = org.key),
          application = Reference(guid = app.guid, key = app.key),
          version = v.version,
          original = originals.get(v.guid).map(originalsModel.toModel),
          service = svc,
          audit = Audit(
            createdAt = v.db.createdAt,
            createdBy = ReferenceGuid(v.db.createdByGuid),
            updatedAt = v.db.createdAt,
            updatedBy = ReferenceGuid(v.db.createdByGuid),
          )
        )
      }
    }.sequence
  }

  private def service(v: InternalVersion): ValidatedNec[String, Service] = {
    servicesDao.findAll(
      versionGuid = Some(v.guid),
      orderBy = Some(OrderBy("-created_at")),
      limit = Some(1),
    ) { q =>
      q.isNull("deleted_at")
    }.headOption
      .toValidNec("Version does not have service json")
      .map(_.json.as[Service])
      .map { service =>
        // Some clients need a value due to apibuilder-validation version 0.52.0
        // having it as a required field.
        service.apidoc match
          case Some(_) => service
          case None => service.copy(apidoc = Some(Apidoc(version = service.version)))
      }
  }
}