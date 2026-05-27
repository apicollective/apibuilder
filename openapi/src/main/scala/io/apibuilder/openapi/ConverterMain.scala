package io.apibuilder.openapi

import io.apibuilder.spec.v0.models.json._
import lib.{ServiceConfiguration, UrlKey}
import play.api.libs.json.Json
import sttp.apispec.openapi.OpenAPI

import java.io.PrintWriter
import java.net.URI
import java.nio.file.{Files, Paths}
import scala.annotation.tailrec
import scala.util.Using

sealed trait OutputMode
object OutputMode {
  case object Report extends OutputMode
  case class Json(outputFile: Option[String] = None) extends OutputMode
}

object ConverterMain {

  private[openapi] case class ParsedOptions(
    input: Option[String] = None,
    organization: Option[String] = None,
    name: Option[String] = None,
    namespace: Option[String] = None,
    version: String = "0.0.1-dev",
    filterHeaders: Set[String] = Set.empty,
    outputMode: OutputMode = OutputMode.Report,
  )

  def main(args: Array[String]): Unit =
    parseArgs(args.toList) match {
      case Left(err) =>
        System.err.println(s"Error: $err")
        System.err.println()
        printUsage()
        sys.exit(1)
      case Right(opts) =>
        run(opts)
    }

  private def run(opts: ParsedOptions): Unit = {
    val uri = resolveInput(opts.input.getOrElse(sys.error("missing required option: input"))) match {
      case Left(err) => System.err.println(s"Error: $err"); sys.exit(1)
      case Right(u) => u
    }

    val openApi = load(uri) match {
      case Left(err) => System.err.println(s"Error: $err"); sys.exit(1)
      case Right(a) => a
    }

    val apiName = opts.name.getOrElse(UrlKey.generate(openApi.info.title))
    val orgNamespace = opts.namespace.getOrElse(inferNamespace(openApi, apiName))
    val config = ServiceConfiguration(
      orgKey = opts.organization.getOrElse(sys.error("missing required option: organization")),
      orgNamespace = orgNamespace,
      version = opts.version,
    )

    val classification = Classification.fromOpenApi(openApi, NamingConfig(), opts.filterHeaders)
    val report = ConversionReport.fromClassification(classification)
    val service = Converter.convert(openApi, config, opts.filterHeaders, opts.name)

    opts.outputMode match {
      case OutputMode.Json(outputFile) =>
        System.err.println(report.summary)
        System.err.println()
        val json = Json.prettyPrint(Json.toJson(service))
        outputFile match {
          case Some(path) =>
            Using(new PrintWriter(path)) { _.write(json) } match {
              case scala.util.Success(_) => System.err.println(s"Written: $path")
              case scala.util.Failure(e) =>
                System.err.println(s"Failed to write $path: ${e.getMessage}")
                sys.exit(1)
            }
          case None =>
            println(json)
        }

      case OutputMode.Report =>
        println(s"Config: apiName=$apiName, namespace=${config.applicationNamespace(apiName)}")
        if (opts.filterHeaders.nonEmpty)
          println(s"Filtering headers: ${opts.filterHeaders.mkString(", ")}")
        println()
        println(report.summary)
        println()
        println(s"Converted: ${service.models.size} models, ${service.enums.size} enums, ${service.unions.size} unions, ${service.resources.size} resources")
        val totalParams = service.resources.flatMap(_.operations).map(_.parameters.size).sum
        println(s"Total parameters across all operations: $totalParams")
    }
  }

  private[openapi] def parseArgs(args: List[String]): Either[String, ParsedOptions] =
    doParse(args).flatMap {
      case p if p.input.isEmpty => Left("input is required")
      case p if p.organization.isEmpty => Left("--organization is required")
      case p => Right(p)
    }

  @tailrec
  private def doParse(args: List[String], acc: ParsedOptions = ParsedOptions()): Either[String, ParsedOptions] =
    args match {
      case Nil => Right(acc)
      case "--organization" :: org :: tail if acc.organization.isEmpty =>
        doParse(tail, acc.copy(organization = Some(org)))
      case "--organization" :: _ :: _ => Left("--organization specified more than once")
      case "--organization" :: Nil => Left("--organization requires a value")
      case "--name" :: name :: tail if acc.name.isEmpty =>
        doParse(tail, acc.copy(name = Some(name)))
      case "--name" :: _ :: _ => Left("--name specified more than once")
      case "--name" :: Nil => Left("--name requires a value")
      case "--namespace" :: ns :: tail if acc.namespace.isEmpty =>
        doParse(tail, acc.copy(namespace = Some(ns)))
      case "--namespace" :: _ :: _ => Left("--namespace specified more than once")
      case "--namespace" :: Nil => Left("--namespace requires a value")
      case "--version" :: v :: tail =>
        doParse(tail, acc.copy(version = v))
      case "--version" :: Nil => Left("--version requires a value")
      case "--filter-header" :: h :: tail =>
        doParse(tail, acc.copy(filterHeaders = acc.filterHeaders + h))
      case "--filter-header" :: Nil => Left("--filter-header requires a value")
      case "--json" :: file :: tail if !file.startsWith("--") =>
        doParse(tail, acc.copy(outputMode = OutputMode.Json(Some(file))))
      case "--json" :: tail =>
        doParse(tail, acc.copy(outputMode = OutputMode.Json()))
      case s :: _ if s.startsWith("--") => Left(s"Unknown flag '$s'")
      case input :: tail if acc.input.isEmpty =>
        doParse(tail, acc.copy(input = Some(input)))
      case input :: _ =>
        Left(s"Unexpected argument '$input' (input already set to '${acc.input.get}')")
    }

  private def resolveInput(input: String): Either[String, URI] =
    if (input.startsWith("http://") || input.startsWith("https://"))
      Right(URI.create(input))
    else {
      val path = Paths.get(input)
      if (Files.exists(path)) Right(path.toAbsolutePath.toUri)
      else Left(s"Not a URL or existing file: $input")
    }

  private def load(uri: URI): Either[String, OpenAPI] = uri.getScheme match {
    case "http" | "https" => OpenApiParser.fromUrl(uri.toString)
    case "file" => OpenApiParser.fromFile(Paths.get(uri))
    case other => Left(s"Unsupported URI scheme: $other")
  }

  private[openapi] def inferNamespace(openApi: OpenAPI, apiName: String): String = {
    val domainParts = openApi.servers.headOption
      .flatMap { server =>
        try {
          val host = URI.create(server.url).getHost
          if (host == null) None
          else {
            val parts = host.split("\\.").toSeq
            val meaningful = parts.filterNot(p => p.startsWith("api") || p == "www" || p == "sandbox")
            val reversed = (if (meaningful.nonEmpty) meaningful else parts).reverse
            Some(reversed)
          }
        } catch {
          case e: IllegalArgumentException =>
            System.err.println(s"Warning: could not parse server URL '${server.url}' for namespace inference: ${e.getMessage}")
            None
        }
      }
      .getOrElse(Seq("io", "apibuilder"))
    (domainParts :+ apiName).mkString(".")
  }

  private def printUsage(): Unit = {
    println("Usage: openapi <input> --organization <org> [options]")
    println()
    println("Input can be:")
    println("  A URL:       https://example.com/openapi.json")
    println("  A file path: /path/to/openapi.json")
    println()
    println("Required:")
    println("  --organization <org>       The apibuilder organization key")
    println()
    println("Options:")
    println("  --json [file]              Output APIBuilder JSON (to file or stdout)")
    println("  --name <name>              Override the inferred API name")
    println("  --namespace <namespace>    Override the inferred API namespace")
    println("  --version <version>        API version (default: 0.0.1-dev)")
    println("  --filter-header <header>   Exclude a header parameter (repeatable)")
    println()
    println("Run via sbt:")
    println("""  sbt "openapi/runMain io.apibuilder.openapi.ConverterMain ./openapi.json --organization myorg --json"""")
  }
}
