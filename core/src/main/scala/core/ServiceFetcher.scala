package core

import com.bryzek.apidoc.spec.v0.models.Service

trait ServiceFetcher {

  def fetch(uri: String): Service

}
