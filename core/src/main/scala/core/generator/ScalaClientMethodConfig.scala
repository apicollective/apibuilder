package core.generator

trait ScalaClientMethodConfig {

  /**
    * The name of the method to call to encode a variable into a path.
    */
  def pathEncodingMethod: String

  /**
    * The name of the method on the response providing the status code.
    */
  def responseStatusMethod: String

  /**
    * The name of the method on the response providing the body.
    */
  def responseBodyMethod: String

  /**
    * The class name for the Response object.
    */
  def responseClass: String

  /**
    * Given a response and a class name, returns code to create an
    * instance of the specified class.
    */
  def toJson(responseName: String, className: String): String

}

object ScalaClientMethodConfigs {

  trait Play extends ScalaClientMethodConfig {
    override def pathEncodingMethod = "play.utils.UriEncoding.encodePathSegment"
    override def responseStatusMethod = "status"
    override def responseBodyMethod = "body"
    override def toJson(responseName: String, className: String) = {
      s"$responseName.json.as[$className]"
    }
  }

  trait Ning extends ScalaClientMethodConfig {
    def packageName: String
    override def pathEncodingMethod = s"_root_.${packageName}.PathSegment.encode"
    override def responseStatusMethod = "getStatusCode"
    override def responseBodyMethod = """getResponseBody("UTF-8")"""
    override def responseClass = "_root_.com.ning.http.client.Response"
    override def toJson(responseName: String, className: String) = {
      s"_root_.${packageName}.Client.parseJson($responseName, _.validate[$className])"
    }
  }

}
