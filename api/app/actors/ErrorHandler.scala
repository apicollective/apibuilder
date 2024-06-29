package actors

import play.api.Logger

/**
  * Common utilities for handling and logging errors in actors
  */
trait ErrorHandler {

  val logger: Logger = Logger(this.getClass)

  /**
    * Wraps a block with error handling that will catch any throwable and log it.
    *
    * Example usage:
    * 
    * class MainActor(name: String) extends Actor with ActorLogging with Util {
    * 
    * def receive = akka.event.LoggingReceive {
    *   
    *   case m @ MainActor.Messages.ExampleMessage => withErrorHandler(m) {
    *     ...
    *   }
    *
    *   case m: Any => logUnhandledMessage(m)
    * 
    * }
    */
  def withErrorHandler[T](
    description: Any
  ) (
    f: => T
  ): Unit = {
    try {
      f
    } catch {
      case t: Throwable => {
        logger.error(msg(s"$description: ${t}") , t)
      }
    }
  }

  /**
    * Wraps a block that will log that the message has been received. Also will
    * catch any throwable and log it.
    *
    * Example usage:
    * 
    * class MainActor(name: String) extends Actor with ActorLogging with Util {
    * 
    * def receive = akka.event.LoggingReceive {
    *   
    *   case m @ MainActor.Messages.ExampleMessage => withVerboseErrorHandler(m) {
    *     ...
    *   }
    *
    *   case m: Any => logUnhandledMessage(m)
    * 
    * }
    */
  def withVerboseErrorHandler[T](
    description: Any
  ) (
    f: => T
  ): Unit = {
    logger.info(msg(description.toString))
    withErrorHandler(description)(f)
  }

  def logUnhandledMessage[T](
    description: Any
  ): Unit = {
    logger.error(msg(s"got an unhandled message: $description"))
  }

  private def msg(value: String) = {
    s"${getClass.getName}: $value"
  }

}
