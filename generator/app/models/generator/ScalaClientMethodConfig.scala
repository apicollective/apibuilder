package generator

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

  def hasModelJsonPackage: Boolean

  /**
    * Given a response and a class name, returns code to create an
    * instance of the specified class.
    */
  def toJson(responseName: String, className: String): String

  /**
   * Given an accessor method name and a type, returns code to create an
   * accessor var.
   */
  def accessor(methodName: String, typeName: String): String
}

object ScalaClientMethodConfigs {

  trait Play extends ScalaClientMethodConfig {
    override val pathEncodingMethod = "play.utils.UriEncoding.encodePathSegment"
    override val responseStatusMethod = "status"
    override val responseBodyMethod = "body"
    override val hasModelJsonPackage = true
    override def toJson(responseName: String, className: String) = {
      s"$responseName.json.as[$className]"
    }
    override def accessor(methodName: String, typeName: String) = {
      s"def ${methodName}: ${typeName} = ${typeName}"
    }
  }

  object Play22 extends Play {
    override val responseClass = "play.api.libs.ws.Response"
  }

  object Play23 extends Play {
    override val responseClass = "play.api.libs.ws.WSResponse"
  }

  trait Ning extends ScalaClientMethodConfig {
    def packageName: String
    override val pathEncodingMethod = s"_root_.${packageName}.PathSegment.encode"
    override val responseStatusMethod = "getStatusCode"
    override val responseBodyMethod = """getResponseBody("UTF-8")"""
    override val responseClass = "_root_.com.ning.http.client.Response"
    override val hasModelJsonPackage = true
    override def toJson(responseName: String, className: String) = {
      s"_root_.${packageName}.Client.parseJson($responseName, _.validate[$className])"
    }
    override def accessor(methodName: String, typeName: String) = {
      s"def ${methodName}: ${typeName} = ${typeName}"
    }
  }

}
