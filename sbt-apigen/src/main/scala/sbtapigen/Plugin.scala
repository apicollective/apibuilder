package sbtapigen

import sbt._
import core.generator.Play2ClientGenerator

object Plugin extends sbt.Plugin {
  import Keys._
  import ApiGenKeys._

  object ApiGenKeys {
    val apiGenerator = config("apigen")

    lazy val apiGenerate = TaskKey[Seq[java.io.File]]("api-generate")
  }

  object ApiGenerator {
    def apply(sourceDir: Seq[sbt.File], outDir: sbt.File, s: TaskStreams): Seq[java.io.File] = {
      s.log.info("Generating APIs")
      for {
        (jsonSource, idx) <- sourceDir.zipWithIndex
        _ = { s.log.debug(s"Reading from ${jsonSource.getName}") }
        jsonStr = IO.read(jsonSource)
        generated = Play2ClientGenerator.apply(jsonStr)
        newFile = outDir / s"Client_${idx}.scala"
      } yield {
        IO.write(newFile, generated)
        s.log.debug(s"Wrote to ${newFile}")
        newFile
      }
    }
  }

  lazy val apiGeneratorSettings: Seq[Setting[_]] = inConfig(Compile)(baseApiGeneratorSettings) ++ Seq(
    // TODO: Figure out dependencies for inside/outside Play.
    libraryDependencies += "com.typesafe.play" %% "play" % "2.2.3"
  )

  lazy val baseApiGeneratorSettings: Seq[Setting[_]] = Seq(
    sourceGenerators <+= sbtapigen.Plugin.ApiGenKeys.apiGenerate,
    apiGenerate <<= apiGenerate in apiGenerator,
    apiGenerate in apiGenerator <<= (clean in apiGenerator, resourceDirectory in apiGenerator, sourceManaged in apiGenerator, streams) map {
      (_, sourceDir, outDir, streams) =>
        ApiGenerator((sourceDir ** "*.json").get, outDir, streams)
    },
    sourceManaged in apiGenerator <<= sourceManaged(_ / "apigen"),
    resourceDirectory in apiGenerator <<= resourceDirectory(_ / "api"),
    clean in apiGenerator <<= (sourceManaged in apiGenerator) map { dir =>
      IO.delete((dir ** "*").get)
    }
  )
}
