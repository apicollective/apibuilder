package lib

import play.api.Play.current

/**
  * Wrapper on play config testing for empty strings and standardizing
  * error message for required configuration.
  */
object Config {

  def requiredString(name: String): String = {
    optionalString(name).getOrElse {
      sys.error(s"configuration parameter[$name] is required")
    }
  }

  def optionalString(name: String): Option[String] = {
    current.configuration.getString(name).map { value =>
      assert(value.trim != "", "Value for configuration parameter[$name] cannot be blank")
      value
    }
  }
}
