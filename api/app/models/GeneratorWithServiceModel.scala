package models

import db.Authorization
import db.generators.{InternalGenerator, InternalGeneratorServicesDao}
import io.apibuilder.api.v0.models.GeneratorWithService
import io.apibuilder.generator.v0.models.Generator

import javax.inject.Inject

class GeneratorWithServiceModel @Inject()(
                                        servicesDao: InternalGeneratorServicesDao,
                                        servicesModel: GeneratorServicesModel
                                        ) {
  def toModel(generator: InternalGenerator): Option[GeneratorWithService] = {
    toModels(Seq(generator)).headOption
  }

  def toModels(generators: Seq[InternalGenerator]): Seq[GeneratorWithService] = {
    val services = servicesDao.findAll(
      guids = Some(generators.map(_.serviceGuid).distinct),
      limit = None,
    ).map { s => s.guid -> servicesModel.toModel(s) }.toMap

    generators.flatMap { g =>
      services.get(g.serviceGuid).map { s =>
        GeneratorWithService(
          s,
          Generator(
            key = g.key,
            name = g.name,
            language = g.language,
            description = g.description,
            attributes = g.attributes
          )
        )
      }
    }
  }
}
