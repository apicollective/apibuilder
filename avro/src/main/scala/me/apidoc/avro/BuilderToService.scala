package me.apidoc.avro

import lib.UrlKey
import com.gilt.apidoc.spec.v0.models._

object BuilderToService {

  def toService(
    name: String,
    orgKey: String,
    applicationKey: String,
    version: String,
    builder: Builder
  ): Service = {
    Service(
      name = name,
      baseUrl = None,
      description = None,
      namespace = "TODO",
      organization = Organization(key = orgKey),
      application = Application(key = applicationKey),
      version = version,
      enums = builder.enums,
      unions = builder.unions,
      models = builder.models,
      imports = Nil,
      headers = Nil,
      resources = Nil
    )
  }
}
