package core.generator

object Target {
  val generator: PartialFunction[String, String => String] = {
    case "ruby-client" => RubyGemGenerator.apply
    case "play-2.3-routes" => Play2RouteGenerator.apply
    case "play-2.3-client" => Play2ClientGenerator.apply
    case "play-2.3-json" => Play2Models.apply
    case "scalacheck-generators" => ScalaCheckGenerators.apply
    case "scala-models" => ScalaCaseClasses.apply
  }

  val description: PartialFunction[String, String] = {
    case "ruby-client" => "A pure ruby library to consume api.json web services. From apidoc, you can download a single file that provides a complete client to a service. The ruby client has minimal dependencies and does not require any additional gems."

    case "play-2.3-routes" => "Apidoc can generate a routes file for the play 2.3 framework. One workflow here is to write the service description in apidoc, upload, copy the routes file, then implement the controllers in play. By code generating the routes file, the compiler tells you if you are missing any operations defined by api.json. While this does not provide full testing (e.g. does not validate the responses), we have found it very pragmatic to ensure services match specifications."

    case "play-2.3-client" => "Play Framework 2.3 offers a very rich <a href=\"http://www.playframework.com/documentation/2.2.x/ScalaWS\">WS API</a>. We are exploring the ability to code generate a client library that will integrate natively with the Play WS API to make it simpler to consume api.json web services."

    case "play-2.3-json" => "Generate a play 2.3 json (includes the output of Scala Models). No need to use this target if you are already using the Play Client target."

    case "scalacheck-generators" => "Generate scalacheck generators for the API models."

    case "scala-models" => "Generate scala models from the API description."

    case "swagger-json" => "We have a prototype to convert api.json to swagger-json. This enables access to all of the great tooling already built on swagger. We specifically made sure the conversion was straight forward as we built api.json and hope to soon have the time to productize the swagger-json generation."

    case "javascript" => "A few teams have expressed interest in a javascript client to api.json, though we are not yet aware of any work in progress."
  }

  val status: PartialFunction[String, String] = {
    case "ruby-client" => "Alpha"
    case "play-2.3-routes" => "Alpha"
    case "play-2.3-client" => "Alpha"
    case "play-2.3-json" => "Alpha"
    case "scalacheck-generators" => "Alpha"
    case "scala-models" => "Alpha"
    case "swagger-json" => "Proposal"
    case "javascript" => "Proposal"
  }

  val values: Seq[String] = Seq(
    "ruby-client",
    "play-2.3-routes",
    "play-2.3-client",
    "play-2.3-json",
    "scalacheck-generators",
    "scala-models",
    "swagger-json",
    "javascript"
  )

  val implemented = values.filter(generator.isDefinedAt)

  def humanize(target: String) = {
    target.split('-').map(_.capitalize).mkString(" ")
  }

  def mechanize(target: String) = {
    target.split(" ").map(_.toLowerCase).mkString("-")
  }
}
