package lib

import javax.inject.Inject

import play.api.{Configuration, Logger}

/**
  * Wrapper on play config testing for empty strings and standardizing
  * error message for required configuration.
  */
class Config @Inject() (
  configuration: Configuration
) {

  def requiredString(name: String): String = {
    optionalString(name).getOrElse {
      val msg = s"configuration parameter[$name] is required"
      Logger.error(msg)
      sys.error(msg)
    }
  }

  def optionalString(name: String): Option[String] = {
    configuration.getOptional[String](name).map { value =>
      if (value.trim == "") {
        val msg = s"Value for configuration parameter[$name] cannot be blank"
        Logger.error(msg)
        sys.error(msg)
      }
      value
    }
  }
}
