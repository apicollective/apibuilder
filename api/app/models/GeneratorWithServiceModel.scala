package models

import db.Authorization
import db.generators.{InternalGenerator, ServicesDao}
import io.apibuilder.api.v0.models.GeneratorWithService

import javax.inject.Inject

class GeneratorWithServiceModel @Inject()(
                                        servicesDao: ServicesDao
                                        ) {
  def toModel(generator: InternalGenerator): Option[GeneratorWithService] = {
    toModels(Seq(generator)).headOption
  }

  def toModels(generators: Seq[InternalGenerator]): Seq[GeneratorWithService] = {
    val services = servicesDao.findAll(
      Authorization.All,
      guids = Some(generators.map(_.serviceGuid).distinct),
      limit = None,
    ).map { s => s.guid -> s }.toMap

    generators.flatMap { g =>
      services.get(g.serviceGuid).map { s =>
        GeneratorWithService(s, g.model)
      }
    }
  }
}
