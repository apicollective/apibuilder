package controllers

import com.gilt.apidocgenerator.models.{ServiceDescription, Generator}
import com.gilt.apidocgenerator.models.json._
import com.gilt.apidocgenerator.Client
import lib.Validation
import play.api.mvc._
import play.api.libs.json._
import core.generator.{CodeGenerator, CodeGenTarget}

object Generators extends Controller {

  def get() = Action { request: Request[AnyContent] =>
    Ok(Json.toJson(targets.filter(_.generator.isDefined).map(t => Generator(t.key, t.name, None, t.description))))
  }

  def getByKey(key: String) = Action { request: Request[AnyContent] =>
    findGenerator(key) match {
      case Some((target, _)) => Ok(Json.toJson(Generator(target.key, target.name, None, target.description)))
      case _ => NotFound
    }
  }

  def postExecuteByKey(key: String) = Action(parse.json) { request: Request[JsValue] =>
    findGenerator(key) match {
      case Some((_, generator)) =>
        request.body.validate[ServiceDescription] match {
          case e: JsError => Conflict(Json.toJson(Validation.error("invalid json document: " + e.toString)))
          case s: JsSuccess[ServiceDescription] => Ok(Json.toJson(generator.generate(s.get)))
        }
      case _ => NotFound
    }
  }

  def findGenerator(key: String): Option[(CodeGenTarget, CodeGenerator)] = for {
    target <- targets.find(_.key == key)
    generator <- target.generator
  } yield(target -> generator)

  val targets = Seq(
      CodeGenTarget(
        key = "ruby_client",
        name = "Ruby client",
        description = Some("A pure ruby library to consume api.json web services. The ruby client has minimal dependencies and does not require any additional gems."),
        status = core.generator.Status.Beta,
        generator = Some(models.RubyClientGenerator)
      ),
      CodeGenTarget(
        key = "ning_1_8_client",
        name = "Ning Async Http Client 1.8",
        description = Some("Ning Async Http v.18 Client - see https://sonatype.github.io/async-http-client"),
        status = core.generator.Status.Alpha,
        generator = Some(models.ning.Ning18ClientGenerator)
      ),
      CodeGenTarget(
        key = "play_2_2_client",
        name = "Play 2.2 client",
        description = Some("Play Framework 2.2 client based on <a href='http://www.playframework.com/documentation/2.2.x/ScalaWS''>WS API</a>. Note this client does NOT support HTTP PATCH."),
        status = core.generator.Status.Beta,
        generator = Some(models.Play22ClientGenerator)
      ),
      CodeGenTarget(
        key = "play_2_3_client",
        name = "Play 2.3 client",
        description = Some("Play Framework 2.3 client based on  <a href='http://www.playframework.com/documentation/2.3.x/ScalaWS'>WS API</a>."),
        status = core.generator.Status.Beta,
        generator = Some(models.Play23ClientGenerator)
      ),
      CodeGenTarget(
        key = "play_2_x_json",
        name = "Play 2.x json",
        description = Some("Generate play 2.x case classes with json serialization based on <a href='http://www.playframework.com/documentation/2.3.x/ScalaJsonCombinators'>Scala Json combinators</a>. No need to use this target if you are already using the Play Client target."),
        status = core.generator.Status.Beta,
        generator = Some(models.Play2Models)
      ),
      CodeGenTarget(
        key = "play_2_x_routes",
        name = "Play 2.x routes",
        description = Some("""Generate a routes file for play 2.x framework. See <a href="/doc/playRoutesFile">Play Routes File</a>."""),
        status = core.generator.Status.Beta,
        generator = Some(models.Play2RouteGenerator)
      ),
      CodeGenTarget(
        key = "scala_models",
        name = "Scala models",
        description = Some("Generate scala models from the API description."),
        status = core.generator.Status.Beta,
        generator = Some(core.generator.ScalaCaseClasses)
      ),
      CodeGenTarget(
        key = "swagger_json",
        name = "Swagger JSON",
        description = Some("Generate a valid swagger 2.0 json description of a service."),
        status = core.generator.Status.Proposal,
        generator = None
      ),
      CodeGenTarget(
        key = "angular",
        name = "AngularJS client",
        description = Some("Generate a simple to use wrapper to access a service from AngularJS"),
        status = core.generator.Status.InDevelopment,
        generator = None
      ),
      CodeGenTarget(
        key = "javascript",
        name = "Javascript client",
        description = Some("Generate a simple to use wrapper to access a service from javascript."),
        status = core.generator.Status.Proposal,
        generator = None
      )
  ).sortBy(_.key)
}
