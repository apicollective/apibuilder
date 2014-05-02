package sbtroutesgen

import sbt._

object Plugin extends sbt.Plugin {
  import Keys._

  val routesGenerator = config("routesgen")
  val routesGenerate: TaskKey[Unit] = TaskKey("routes-generate")

  val customRoutesSep =
"""
  |
  |#-----------------------------------
  |# Custom routes                    |
  |#-----------------------------------
""".stripMargin

  private def generateRoutes(api: String): String = {
    import core.generator.Play2RouteGenerator
    import core.ServiceDescription

    val sd = ServiceDescription(api)
    Play2RouteGenerator(sd).generate()
  }

  private def createRoutes(api: sbt.File, confDirectory: sbt.File): Unit = {
    val json = IO.read(api)
    val routes = generateRoutes(json)
    val outputRoutes = confDirectory / "routes"
    IO.write(outputRoutes, routes)
  }

  private def appendCustomRoutes(confDirectory: sbt.File) = {
    val routesFile = confDirectory / "routes"
    val customRouteFile = confDirectory / "custom.Route"
    if(customRouteFile.exists())
      IO.write(routesFile, customRoutesSep + IO.read(customRouteFile), append = true)
  }

  lazy val routesGeneratorSettings: Seq[Setting[_]] = Seq(
    sourceDirectory in routesGenerator <<= baseDirectory(_ / "api"),
    routesGenerate in routesGenerator <<= (sourceDirectory in routesGenerator, baseDirectory) map {
      (sourceDir, baseDirectory) =>
        val confDirectory = baseDirectory / "conf"
        createRoutes((sourceDir ** "*.json").get.head, confDirectory)
        appendCustomRoutes(confDirectory)
    }
  )
}
