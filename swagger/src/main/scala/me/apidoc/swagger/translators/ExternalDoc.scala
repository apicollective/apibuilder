package me.apidoc.swagger.translators

import me.apidoc.swagger.Util
import com.wordnik.swagger.{ models => swagger }

object ExternalDoc {

  def apply(docs: Option[swagger.ExternalDocs]): Option[String] = {
    docs.flatMap { doc =>
      Util.combine(Seq(Option(doc.getDescription), Option(doc.getUrl)), ": ")
    }
  }

}
