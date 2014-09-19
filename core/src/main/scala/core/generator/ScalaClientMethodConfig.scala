package core.generator

case class ScalaClientMethodConfig(

  /**
    * The name of the method to call to encode a variable into a path.
    */
  pathEncodingMethod: String,

  /**
    * The name of the method on the response providing the status code.
    */
  responseStatusMethod: String

)

object ScalaClientMethodConfigs {

  val Play = ScalaClientMethodConfig(
    pathEncodingMethod = "play.utils.UriEncoding.encodePathSegment",
    responseStatusMethod = "status"
  )

  val Ning = ScalaClientMethodConfig(
    pathEncodingMethod = "String.toString", // TODO
    responseStatusMethod = "getStatusCode"
  )

}
