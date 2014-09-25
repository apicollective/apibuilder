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

  /**
   * Given an accessor method name and a type, returns code to create an
   * accessor var.
   */
  def accessor(methodName: String, typeName: String): String

  def hasModelJsonPackage: Boolean = true
}

object ScalaClientMethodConfigs {

  trait Play extends ScalaClientMethodConfig {
    override val pathEncodingMethod = "play.utils.UriEncoding.encodePathSegment"
    override val responseStatusMethod = "status"
    override val responseBodyMethod = "body"
    override def toJson(responseName: String, className: String) = {
      s"$responseName.json.as[$className]"
    }
    override def accessor(methodName: String, typeName: String) = {
      s"def ${methodName}: ${typeName} = ${typeName}"
    }
  }

  trait Ning extends ScalaClientMethodConfig {
    def packageName: String
    override val pathEncodingMethod = "_encodePathParameter"
    override val responseStatusMethod = "getStatusCode"
    override val responseBodyMethod = """getResponseBody("UTF-8")"""
    override val responseClass = "com.ning.http.client.Response"
    override def toJson(responseName: String, className: String) = {
      s"${packageName}.Client.parseJson($responseName, _.validate[$className])"
    }
    override def accessor(methodName: String, typeName: String) = {
      s"def ${methodName}: ${typeName} = ${typeName}"
    }
  }


  object Commons extends ScalaClientMethodConfig {
    override val pathEncodingMethod = ""
    override val responseStatusMethod = "statusCode"
    override val responseBodyMethod = """bodyToString"""
    override val responseClass = "com.gilt.commons.client.Response"
    override def toJson(responseName: String, className: String) = {
      s"com.gilt.commons.json.CommonsJson.parse[$className](response.body)"
    }
    override def accessor(methodName: String, typeName: String) = {
      s"def ${methodName}: ${typeName}"
    }
    override val hasModelJsonPackage = false
  }
}
