package lib

import org.pegdown.PegDownProcessor

/**
  * Wrapper on play config testing for empty strings and standardizing
  * error message for required configuration.
  */
object Markdown {

  def toHtml(value: String): String = {
    (new PegDownProcessor()).markdownToHtml(value)
  }

}
