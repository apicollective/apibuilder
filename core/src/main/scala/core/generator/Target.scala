package core.generator

import com.gilt.apidoc.models.OrganizationMetadata
import core.{ServiceDescription, Text}

sealed trait Status

object Status {
  case object Alpha extends Status
  case object Beta extends Status
  case object Proposal extends Status
}

case class Target(key: String, name: String, description: String, status: Status) {
 
 def userAgent(apidocVersion: String, orgKey: String, serviceKey: String, serviceVersion: String): String = {
    // USER_AGENT = "apidoc:0.4.64 http://www.apidoc.me/gilt/code/mercury-generic-warehouse-api/0.0.3-dev/ruby_client"
    s"apidoc:$apidocVersion http://www.apidoc.me/$orgKey/code/$serviceKey/$serviceVersion/$key"
  }

}

object Target {

  val All = Seq(
    Target(
      key = "ruby_client",
      name = "Ruby client",
      description = "A pure ruby library to consume api.json web services. The ruby client has minimal dependencies and does not require any additional gems.",
      status = Status.Beta
    ),
    Target(
      key = "ning_1_8_client",
      name = "Ning Async Http Client 1.8",
      description = "Ning Async Http v.18 Client - see https://sonatype.github.io/async-http-client",
      status = Status.Alpha
    ),
    Target(
      key = "play_2_2_client",
      name = "Play 2.2 client",
      description = "Play Framework 2.2 client based on <a href='http://www.playframework.com/documentation/2.2.x/ScalaWS''>WS API</a>. Note this client does NOT support HTTP PATCH.",
      status = Status.Beta
    ),
    Target(
      key = "play_2_3_client",
      name = "Play 2.3 client",
      description = "Play Framework 2.3 client based on  <a href='http://www.playframework.com/documentation/2.3.x/ScalaWS'>WS API</a>.",
      status = Status.Beta
    ),
    Target(
      key = "play_2_x_json",
      name = "Play 2.x json",
      description = "Generate play 2.x case classes with json serialization based on <a href='http://www.playframework.com/documentation/2.3.x/ScalaJsonCombinators'>Scala Json combinators</a>. No need to use this target if you are already using the Play Client target.",
      status = Status.Beta
    ),
    Target(
      key = "play_2_x_routes",
      name = "Play 2.x routes",
      description = "Generate a routes file for play 2.x framework.",
      status = Status.Beta
    ),
    Target(
      key = "scala_models",
      name = "Scala models",
      description = "Generate scala models from the API description.",
      status = Status.Beta
    ),
    Target(
      key = "swagger_json",
      name = "Swagger JSON",
      description = "Generate a valid swagger 2.0 json description of a service.",
      status = Status.Proposal
    ),
    Target(
      key = "javascript",
      name = "Javascript client",
      description = "Generate a simple to use wrapper to access a service from javascript.",
      status = Status.Proposal
    ),
    Target(
      key = "commons_6_client",
      name = "Commons 6 client",
      description = "Gilt internal client based on commons 6",
      status = Status.Beta
    )
  )

  val Implemented = All.filter(_.status != Status.Proposal)

  def generate(target: Target, apidocVersion: String, orgKey: String, metadata: Option[OrganizationMetadata], sd: ServiceDescription, serviceKey: String, serviceVersion: String): String = {
    val userAgent = target.userAgent(apidocVersion, orgKey, serviceKey, serviceVersion)
    lazy val ssd = new ScalaServiceDescription(sd, metadata)
    target.key match {
      case "ruby_client" => RubyClientGenerator.generate(sd, userAgent)
      case "ning_1_8_client" => ning.NingClientGenerator.generate(ning.NingVersions.V1_8_x, ssd, userAgent)
      case "play_2_2_client" => Play2ClientGenerator.generate(PlayFrameworkVersions.V2_2_x, ssd, userAgent)
      case "play_2_3_client" => Play2ClientGenerator.generate(PlayFrameworkVersions.V2_3_x, ssd, userAgent)
      case "play_2_x_routes" => Play2RouteGenerator.generate(sd)
      case "play_2_x_json" => Play2Models.apply(ssd)
      case "scala_models" => ScalaCaseClasses.generate(ssd)
      case "commons_6_client" => Commons6ClientGenerator.generate(ssd, userAgent)
      case (other) => {
        sys.error(s"unsupported code generation for target[$other]")
      }
    }
  }

  def findByKey(key: String): Option[Target] = All.find(_.key == key)

}
