package core.generator

import core.ServiceDescription

sealed trait Status

object Status {
  case object Alpha extends Status
  case object Beta extends Status
  case object Proposal extends Status
}

case class Target(key: String, name: String, description: String, status: Status)

object Target {

  val All = Seq(
    Target(
      key = "ruby_client",
      name = "Ruby client",
      description = "A pure ruby library to consume api.json web services. From apidoc, you can download a single file that provides a complete client to a service. The ruby client has minimal dependencies and does not require any additional gems.",
      status = Status.Alpha
    ),
    Target(
      key = "play_2_3_routes",
      name = "Play 2.3 routes",
      description = "Apidoc can generate a routes file for the play 2.3 framework. One workflow here is to write the service description in apidoc, upload, copy the routes file, then implement the controllers in play. By code generating the routes file, the compiler tells you if you are missing any operations defined by api.json. While this does not provide full testing (e.g. does not validate the responses), we have found it very pragmatic to ensure services match specifications.",
      status = Status.Alpha
    ),
    Target(
      key = "play_2_3_client",
      name = "Play 2.3 client",
      description = "Play Framework 2.3 offers a very rich <a href='http://www.playframework.com/documentation/2.3.x/ScalaWS'>WS API</a>. We are exploring the ability to code generate a client library that will integrate natively with the Play WS API to make it simpler to consume api.json web services.",
      status = Status.Alpha
    ),
    Target(
      key = "play_2_3_json",
      name = "Play 2.3 json",
      description = "Generate a play 2.3 json (includes the output of Scala Models). No need to use this target if you are already using the Play Client target.",
      status = Status.Alpha
    ),
    Target(
      key = "scala_models",
      name = "Scala models",
      description = "Generate scala models from the API description.",
      status = Status.Alpha
    ),
    Target(
      key = "avro_schema",
      name = "Avro schemas",
      description = "Generates AVRO JSON schema for the models in this service.",
      status = Status.Alpha
    ),
    Target(
      key = "swagger_json",
      name = "Swagger JSON",
      description = "We have a prototype to convert api.json to swagger-json. This enables access to all of the great tooling already built on swagger. We specifically made sure the conversion was straight forward as we built api.json and hope to soon have the time to productize the swagger-json generation.",
      status = Status.Proposal
    ),
    Target(
      key = "javascript",
      name = "Javascript client",
      description = "A few teams have expressed interest in a javascript client to api.json, though we are not yet aware of any work in progress.",
      status = Status.Proposal
    )
  )

  val Implemented = All.filter(_.status != Status.Proposal)

  def generate(target: Target, sd: ServiceDescription): String = {
    target.key match {
      case "ruby_client" => RubyGemGenerator.generate(sd)
      case "play_2_3_routes" => Play2RouteGenerator.generate(sd)
      case "play_2_3_client" => Play2ClientGenerator.apply(sd)
      case "play_2_3_json" => Play2Models.apply(sd)
      case "scala_models" => ScalaCaseClasses.apply(sd)
      case "avro_schema" => AvroSchemas.apply(sd)
      case (other) => {
        sys.error(s"unsupported code generation for target[$other]")
      }
    }
  }

  def findByKey(key: String): Option[Target] = All.find(_.key == key)

}
