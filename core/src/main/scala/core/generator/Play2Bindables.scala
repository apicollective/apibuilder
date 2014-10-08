package core.generator

import core.Text

object Play2Bindables {

  def build(
    ssd: ScalaServiceDescription
  ): Option[String] = {
    import Text._

    ssd.enums match {
      case Nil => None
      case enums => {
        Some(
          Seq(
            "object Bindables {",
            "  import play.api.mvc.QueryStringBindable",
            "  import play.api.mvc.PathBindable",
            s"  import ${ssd.packageName}.models._",
            enums.map( buildImplicit(_) ).mkString("\n\n").indent(2),
            "}"
          ).mkString("\n\n")
        )
      }
    }
  }

  private[generator] def buildImplicit(
    enum: ScalaEnum
  ): String = {
    s"// ${enum.name}\n" +
    """private val enum%sNotFound = (key: String, e: Exception) => s"Unrecognized $key, should be one of ${%s.all.mkString(", ")}"""".format(enum.name, enum.name) +
    s"""

implicit val pathBindableEnum${enum.name} = new PathBindable.Parsing[${enum.name}] (
  ${enum.name}.fromString(_).get, _.toString, enum${enum.name}NotFound
)

implicit val queryStringBindableEnum${enum.name} = new QueryStringBindable.Parsing[${enum.name}](
  ${enum.name}.fromString(_).get, _.toString, enum${enum.name}NotFound
)"""
  }

}
