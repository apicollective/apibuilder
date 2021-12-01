package io.apibuilder.swagger.translators

import io.apibuilder.swagger.Util
import io.swagger.{ models => swagger }

object ExternalDoc {

  def apply(docs: Option[swagger.ExternalDocs]): Option[String] = {
    docs.flatMap { doc =>
      Util.combine(Seq(Option(doc.getDescription), Option(doc.getUrl)), ": ")
    }
  }

}
