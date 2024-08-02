package models

import db.Authorization
import db.generators.InternalGeneratorService
import io.apibuilder.api.v0.models.{GeneratorService, GeneratorWithService}
import io.apibuilder.common.v0.models.{Audit, ReferenceGuid}

import javax.inject.Inject

class GeneratorServicesModel @Inject()() {
  def toModel(generator: InternalGeneratorService): GeneratorService = {
    toModels(Seq(generator)).head
  }

  def toModels(services: Seq[InternalGeneratorService]): Seq[GeneratorService] = {
    services.map { s =>
      GeneratorService(
        guid = s.guid,
        uri = s.uri,
        audit = Audit(
          createdAt = s.db.createdAt,
          createdBy = ReferenceGuid(s.db.createdByGuid),
          updatedAt = s.db.updatedAt,
          updatedBy = ReferenceGuid(s.db.createdByGuid),
        )
      )
    }
  }
}
