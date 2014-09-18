package core.generator

case class ScalaClientMethodConfig(

  /**
    * The name of the method to call to encode a variable into a path.
    */
  pathEncodingMethod: String
)

object ScalaClientMethodConfigs {

  val Play = ScalaClientMethodConfig(
    pathEncodingMethod = "play.utils.UriEncoding.encodePathSegment"
  )

  val Ning = ScalaClientMethodConfig(
    pathEncodingMethod = "String.toString" // TODO
  )

}
