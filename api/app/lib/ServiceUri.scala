package lib

case class ServiceUri(
  host: String,
  org: String,
  app: String,
  version: String
)

/**
  * Parses the URI for a service.json file into its component parts
  */
object ServiceUri {

  private val Pattern = """^https?:\/\/([^\/]+)/([^\/]+)/([^\/]+)/([^\/]+)\/service.json$""".r

  def parse(uri: String): Option[ServiceUri] = {
    uri.toLowerCase.trim match {
      case Pattern(host, org, app, version) => {
        Some(
          ServiceUri(
            host = host,
            org = org,
            app = app,
            version = version
          )
        )
      }

      case _ => {
        None
      }
    }
  }

}
