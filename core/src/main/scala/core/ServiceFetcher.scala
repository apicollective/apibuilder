package core

import com.gilt.apidoc.spec.v0.models.Service

trait ServiceFetcher {

  def fetch(uri: String): Service

}
