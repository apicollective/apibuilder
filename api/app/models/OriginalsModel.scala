package models

import builder.api_json.upgrades.ServiceParser
import cats.data.Validated.{Invalid, Valid}
import cats.data.ValidatedNec
import cats.implicits.*
import db.*
import io.apibuilder.api.v0.models.Original
import io.apibuilder.common.v0.models.Reference
import io.apibuilder.spec.v0.models.Service

import javax.inject.Inject

class OriginalsModel @Inject()() {

  def toModel(v: InternalOriginal): Original = {
    toModels(Seq(v)).head
  }

  def toModels(originals: Seq[InternalOriginal]): Seq[Original] = {
    originals.map { o =>
      Original(
        `type` = o.`type`,
        data = o.data,
      )
    }
  }
}