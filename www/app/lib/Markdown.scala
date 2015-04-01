package lib

import org.pegdown.{Extensions, PegDownProcessor}

/**
  * Wrapper on play config testing for empty strings and standardizing
  * error message for required configuration.
  */
object Markdown {

  def toHtml(value: String): String = {
    (new PegDownProcessor(Extensions.ALL)).markdownToHtml(value)
  }

}
