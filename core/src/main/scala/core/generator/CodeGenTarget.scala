package core.generator

import core.plugin.PluginLoader

case class CodeGenTarget(key: String, name: String, description: Option[String], status: Status, generator: Option[CodeGenerator])

object CodeGenTarget {

  lazy val All = {
    val staticGenerators = Seq(
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
        description = Some("""Generate a routes file for play 2.x framework. See <a href="/doc/playRoutesFile">Play Routes File</a>."""),
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
        key = "angular",
        name = "AngularJS client",
        description = Some("Generate a simple to use wrapper to access a service from AngularJS"),
        status = Status.InDevelopment,
        generator = None
      ),
      CodeGenTarget(
        key = "javascript",
        name = "Javascript client",
        description = Some("Generate a simple to use wrapper to access a service from javascript."),
        status = Status.Proposal,
        generator = None
      )
    )

    val pluginGenerators = PluginLoader.loadPlugins[CodeGeneratorPlugin]("codegen").map(_.getTarget)

    (staticGenerators ++ pluginGenerators).sortBy(_.key)
  }

  lazy val Implemented = All.filter(target => target.status != Status.Proposal && target.generator.isDefined)

  def findByKey(key: String): Option[CodeGenTarget] = All.find(_.key == key)

}
