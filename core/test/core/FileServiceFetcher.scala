package core

import io.apibuilder.spec.v0.models.Service
import io.apibuilder.spec.v0.models.json._
import play.api.libs.json.Json
import java.net.URI

case class FileServiceFetcher() extends ServiceFetcher {

  override def fetch(uri: String): Service = {
    val source = scala.io.Source.fromURI(new URI(uri))
    try {
      val contents = source.getLines().mkString
      Json.parse(contents).as[Service]
    } finally {
      source.close()
    }
  }

}
