package gilt

import net.virtualvoid.sbt.graph.{ Plugin => GraphPlugin }
import sbt._

/**
 * This is broken out into a separate module because it used to not work across
 * 0.11.3.  It does now, but no reason not to encapsulate this a little bit so
 * it can behave differently in different versions going forward.
 */
object DependencyGraph {
  val settings: Seq[Project.Setting[_]] =
    GraphPlugin.graphSettings ++
    Seq(Compile, Test, Runtime, Provided, Optional).flatMap(addDependencySvgSettings)

  lazy val dependencySvg = TaskKey[File](
    "dependency-svg",
    "Creates an SVG file containing the dependency-graph for a project (based on dependency-dot, requires graphviz tools)"
  )
  lazy val dependencySvgView = TaskKey[Unit](
    "dependency-svg-view",
    "Displays jar dependencies in a browser, based on dependency-dot, requires graphviz tools)"
  )

  // Adding to each config (as opposed to globally) to match what's going on
  // in virtualvoid plugin itself. When added globally, compiler complains
  // that dependencies are not defined.
  // [error]   *:dependency-dot from *:dependency-svg-view
  // [error]      Did you mean optional:dependency-dot ?
  private[this] def addDependencySvgSettings(config: Configuration): Seq[Setting[_]] = {
    inConfig(config) { seq(
      dependencySvg <<=  dependencySvgFile,
      dependencySvgView <<=  dependencySvgViewTask
    ) }
  }

  private[this] def dependencySvgViewTask = dependencySvgFile map { (svgFile: File) =>
    // safari displays them nicely and allows for search/zoom
    val safariCmd = Seq("open", "-a", "Safari", svgFile.absolutePath)
    safariCmd.!
    ()
  }

  private[this] def dependencySvgFile = GraphPlugin.dependencyDot map { (dotFile: File) =>
    val targetSvgFileName: File = dotFile.getParentFile / (dotFile.base + ".svg")
    val dotCmd = Seq("dot", "-o"+targetSvgFileName.absolutePath, "-Tsvg", dotFile.absolutePath)
    dotCmd.!
    targetSvgFileName
  }
}
