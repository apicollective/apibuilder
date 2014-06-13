package core.generator

import scala.util.Try

object Target {
  def generator(target: String) = Try[String => String] {
    target match {
      case "ruby-client" => RubyGemGenerator.apply
      case "play-2.3-routes" => Play2RouteGenerator.apply
      case "play-2.3-client" => Play2ClientGenerator.apply
      case "play-2.3-json" => Play2Models.apply
      case "scalacheck-generators" => ScalaCheckGenerators.apply
      case "scala-models" => ScalaCaseClasses.apply
    }
  }

  def values: Seq[String] = Seq(
    "ruby-client",
    "play-2.3-routes",
    "play-2.3-client",
    "play-2.3-json",
    "scalacheck-generators",
    "scala-models"
  )

  def humanize(target: String) = {
    target.split('-').map(_.capitalize).mkString(" ")
  }
}
