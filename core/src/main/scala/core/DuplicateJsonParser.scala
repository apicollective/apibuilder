package core

import org.typelevel.jawn.Facade.SimpleFacade
import org.typelevel.jawn.Parser
import play.api.libs.json._

import scala.util.{Failure, Success}

object DuplicateJsonParser {
  implicit object PlayJsonFacade extends SimpleFacade[JsValue] {
    override def jnull = JsNull
    override def jfalse = JsFalse
    override def jtrue = JsTrue

    override def jnum(s: CharSequence, decIndex: Int, expIndex: Int): JsValue = {
      JsNumber(BigDecimal(String.valueOf(s)))
    }
    override def jstring(s: CharSequence): JsValue = JsString(String.valueOf(s))
    override def jarray(vs: List[JsValue]) = JsArray(vs)
    override def jobject(vs: Map[String, JsValue]) = JsObject(vs)
  }

  def foo(value: String): Seq[String] = {
    new JawnParser()
    Parser.parseFromString(value) match {
      case Success(js) => {
        println(s"JS: $js")
        Nil
      }
      case Failure(_) => Nil
    }
  }
}
