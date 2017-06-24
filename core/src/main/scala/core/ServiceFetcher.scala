package core

import io.apibuilder.apidoc.spec.v0.models.Service

trait ServiceFetcher {

  def fetch(uri: String): Service

}
