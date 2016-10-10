package actors

import play.api.Logger

/**
  * Common utilities for handling and logging errors in actors
  */
trait ErrorHandler {

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
  ) {
    try {
      f
    } catch {
      case t: Throwable => {
        Logger.error(msg(s"$description: ${t}") , t)
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
  ) {
    Logger.info(msg(description.toString))
    withErrorHandler(description)(f)
  }

  def logUnhandledMessage[T](
    description: Any
  ) {
    Logger.error(msg(s"got an unhandled message: $description"))
  }

  private[this] def msg(value: String) = {
    s"${getClass.getName}: $value"
  }

}
