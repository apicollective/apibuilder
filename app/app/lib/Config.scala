package lib

import javax.inject.{Inject, Singleton}

import play.api.{Configuration, Logger}

/**
  * Wrapper on play config testing for empty strings and standardizing
  * error message for required configuration.
  */
@Singleton
class Config @Inject() (
  configuration: Configuration
) {

  private val logger: Logger = Logger(this.getClass)

  def requiredString(name: String): String = {
    optionalString(name).getOrElse {
      val msg = s"configuration parameter[$name] is required"
      logger.error(msg)
      sys.error(msg)
    }
  }

  def optionalString(name: String): Option[String] = {
    configuration.getOptional[String](name).map { value =>
      value.trim match {
        case "" => {
          val msg = s"Value for configuration parameter[$name], if specified, cannot be blank"
          logger.error(msg)
          sys.error(msg)
        }
        case v => v
      }
    }
  }
}
