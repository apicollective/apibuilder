package lib

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet

/**
  * Wrapper on play config testing for empty strings and standardizing
  * error message for required configuration.
  */
object Markdown {

  def apply(
    value: Option[String],
    default: String = ""
  ): String = {
    value.map { toHtml }.getOrElse(default)
  }

  private[this] val options = new MutableDataSet()
  private[this] val parser = Parser.builder(options).build
  private[this] val renderer = HtmlRenderer.builder(options).build

  def toHtml(value: String): String = {
    renderer.render(parser.parse(value))
  }

}
