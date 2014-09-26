package core.generator

import com.gilt.apidoc.models.OrganizationMetadata
import core.{ServiceDescription, Text}


case class CodeGenTarget(key: String, name: String, description: Option[String], status: Status, generator: Option[CodeGenerator]) {
 
 def userAgent(apidocVersion: String, orgKey: String, serviceKey: String, serviceVersion: String): String = {
    // USER_AGENT = "apidoc:0.4.64 http://www.apidoc.me/gilt/code/mercury-generic-warehouse-api/0.0.3-dev/ruby_client"
    s"apidoc:$apidocVersion http://www.apidoc.me/$orgKey/code/$serviceKey/$serviceVersion/$key"
  }

  def supportsOrganization(org: String): Boolean = true

}

object CodeGenTarget {

  val All = Seq(
    CodeGenTarget(
      key = "ruby_client",
      name = "Ruby client",
      description = Some("A pure ruby library to consume api.json web services. The ruby client has minimal dependencies and does not require any additional gems."),
      status = Status.Beta,
      generator = Some(core.generator.RubyClientGenerator)
    ),
    CodeGenTarget(
      key = "ning_1_8_client",
      name = "Ning Async Http Client 1.8",
      description = Some("Ning Async Http v.18 Client - see https://sonatype.github.io/async-http-client"),
      status = Status.Alpha,
      generator = Some(core.generator.ning.Ning18ClientGenerator)
    ),
    CodeGenTarget(
      key = "play_2_2_client",
      name = "Play 2.2 client",
      description = Some("Play Framework 2.2 client based on <a href='http://www.playframework.com/documentation/2.2.x/ScalaWS''>WS API</a>. Note this client does NOT support HTTP PATCH."),
      status = Status.Beta,
      generator = Some(core.generator.Play22ClientGenerator)
    ),
    CodeGenTarget(
      key = "play_2_3_client",
      name = "Play 2.3 client",
      description = Some("Play Framework 2.3 client based on  <a href='http://www.playframework.com/documentation/2.3.x/ScalaWS'>WS API</a>."),
      status = Status.Beta,
      generator = Some(core.generator.Play23ClientGenerator)
    ),
    CodeGenTarget(
      key = "play_2_x_json",
      name = "Play 2.x json",
      description = Some("Generate play 2.x case classes with json serialization based on <a href='http://www.playframework.com/documentation/2.3.x/ScalaJsonCombinators'>Scala Json combinators</a>. No need to use this target if you are already using the Play Client target."),
      status = Status.Beta,
      generator = Some(core.generator.Play2Models)
    ),
    CodeGenTarget(
      key = "play_2_x_routes",
      name = "Play 2.x routes",
      description = Some("Generate a routes file for play 2.x framework."),
      status = Status.Beta,
      generator = Some(core.generator.Play2RouteGenerator)
    ),
    CodeGenTarget(
      key = "scala_models",
      name = "Scala models",
      description = Some("Generate scala models from the API description."),
      status = Status.Beta,
      generator = Some(core.generator.ScalaCaseClasses)
    ),
    CodeGenTarget(
      key = "swagger_json",
      name = "Swagger JSON",
      description = Some("Generate a valid swagger 2.0 json description of a service."),
      status = Status.Proposal,
      generator = None
    ),
    CodeGenTarget(
      key = "javascript",
      name = "Javascript client",
      description = Some("Generate a simple to use wrapper to access a service from javascript."),
      status = Status.Proposal,
      generator = None
    )
  ).sortBy(_.key)

  val Implemented = All.filter(target => target.status != Status.Proposal && target.generator.isDefined)

  def generate(target: CodeGenTarget, apidocVersion: String, orgKey: String, metadata: Option[OrganizationMetadata], sd: ServiceDescription, serviceKey: String, serviceVersion: String): String = {
    val userAgent = target.userAgent(apidocVersion, orgKey, serviceKey, serviceVersion)
    lazy val ssd = new ScalaServiceDescription(sd, metadata)
    target.generator.fold(sys.error(s"unsupported code generation for target[$target.key]"))(_.generate(ssd, userAgent))
  }

  def findByKey(key: String): Option[CodeGenTarget] = All.find(_.key == key)

}
