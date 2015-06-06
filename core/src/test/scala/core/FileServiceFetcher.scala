package core

import com.bryzek.apidoc.spec.v0.models.Service
import com.bryzek.apidoc.spec.v0.models.json._
import play.api.libs.json.Json
import java.net.URI

case class FileServiceFetcher() extends ServiceFetcher {

  override def fetch(uri: String): Service = {
    val contents = scala.io.Source.fromURI(new URI(uri)).getLines.mkString
    Json.parse(contents).as[Service]
  }

}
