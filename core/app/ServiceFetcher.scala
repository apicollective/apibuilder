package core

import io.apibuilder.spec.v0.models.Service

trait ServiceFetcher {

  def fetch(uri: String): Service

}
