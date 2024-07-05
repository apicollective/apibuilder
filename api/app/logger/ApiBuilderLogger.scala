package logger

import cats.data.NonEmptyChain
import com.rollbar.notifier.Rollbar
import com.rollbar.notifier.config.ConfigBuilder
import lib.AppConfig
import org.slf4j.{Logger, LoggerFactory}
import play.api.{Environment, Mode}

import java.util
import javax.inject.Inject
import scala.jdk.CollectionConverters._

trait CommonLogger {
  def withKeyValue(name: String, value: String): CommonLogger
  def withKeyValues(name: String, values: Seq[String], max: Int = 10): CommonLogger

  def info(msg: String): Unit
  def warn(msg: String): Unit
  def warn(ex: Throwable, msg: String): Unit
  def error(msg: String): Unit
  def error(ex: Throwable, msg: String): Unit

  final def withKeyValue(name: String, value: Boolean): CommonLogger = withKeyValue(name, value.toString)
  final def withKeyValue(name: String, value: Int): CommonLogger = withKeyValue(name, value.toString)
  final def withKeyValue(name: String, value: Long): CommonLogger = withKeyValue(name, value.toString)
  final def withKeyValue(name: String, value: BigDecimal): CommonLogger = withKeyValue(name, value.toString)
  final def withKeyValue(name: String, value: Option[String]): CommonLogger = {
    value match {
      case None => this
      case Some(v) => withKeyValue(name, v)
    }
  }

  final def withKeyValues(name: String, values: NonEmptyChain[String]): CommonLogger = {
    withKeyValues(name, values.toNonEmptyList.toList)
  }
}

class ApiBuilderLogger @Inject() (
  env: Environment,
  config: AppConfig
) extends CommonLogger {

  private lazy val logger = LoggerFactory.getLogger("application")

  private lazy val rollbar: Rollbar = {
    val config = ConfigBuilder
      .withAccessToken(hoaConfig.rollbarConfig.accessToken)
      .environment(env.mode.toString)
      .build
    Rollbar.init(config)
  }

  private def builder: CommonLogger = AcumenLoggerBuilder(
    mode = env.mode,
    logger = logger,
    rollbar = rollbar,
    enableRollbar = hoaConfig.rollbarConfig.enabled
  ).withKeyValue("environment", env.mode.toString)

  override def withKeyValue(name: String, value: String): CommonLogger = {
    builder.withKeyValue(name, value)
  }

  override def withKeyValues(name: String, values: Seq[String], max: Int = 10): CommonLogger = {
    builder.withKeyValues(name, values.take(max))
  }

  override def info(msg: String): Unit = builder.info(msg)
  override def warn(msg: String): Unit = builder.warn(msg)
  override def warn(ex: Throwable, msg: String): Unit = builder.warn(ex, msg)
  override def error(msg: String): Unit = builder.warn(msg)
  override def error(ex: Throwable, msg: String): Unit = builder.warn(ex, msg)

}

case class AcumenLoggerBuilder(
  mode: Mode,
  logger: Logger,
  rollbar: Rollbar,
  enableRollbar: Boolean,
  keyValues: Map[String, String] = Map.empty
) extends CommonLogger {

  override def withKeyValue(name: String, value: String): AcumenLoggerBuilder = {
    this.copy(
      keyValues = keyValues ++ Map(name -> value)
    )
  }

  override def withKeyValues(name: String, values: Seq[String], max: Int = 10): AcumenLoggerBuilder = {
    values.toList match {
      case one :: Nil => withKeyValue(name, one)
      case _ =>
        this.copy(
          keyValues = keyValues ++ values.take(max).zipWithIndex.map { case (v, i) => (s"${name}_${i}", v) }
        )
    }
  }

  override def info(msg: String): Unit = {
    logger.info(build(msg, None))
  }

  override def warn(msg: String): Unit = {
    logger.warn(build(msg, None))
    ifRollbar {
      rollbar.warning(msg, keyValuesToJava)
    }
  }
  override def warn(ex: Throwable, msg: String): Unit = {
    logger.warn(build(msg, Some(ex)), ex)
    ifRollbar {
      rollbar.warning(ex, keyValuesToJava, msg)
    }
  }

  override def error(msg: String): Unit = {
    logger.error(build(msg, None))
    ifRollbar {
      rollbar.error(msg, keyValuesToJava)
    }
  }

  override def error(ex: Throwable, msg: String): Unit = {
    logger.error(build(msg, Some(ex)), ex)
    ifRollbar {
      rollbar.error(ex, keyValuesToJava, msg)
    }
  }

  private def ifRollbar(function: => Any): Unit = {
    mode match {
      case Mode.Prod if enableRollbar => {
        function
        ()
      }
      case _ => // no-op
    }
  }

  private def keyValuesObjects: Map[String, Object] = keyValues
  private def keyValuesToJava: util.Map[String, Object] = keyValuesObjects.asJava

  private def build(msg: String, ex: Option[Throwable]): String = {
    val exMsg = ex match {
      case None => ""
      case Some(e) => {
        e.printStackTrace(System.err)
        s" ${e.getMessage}"
      }
    }
    val sb = new StringBuilder()
    sb.append(s"$msg$exMsg")
    if (keyValues.nonEmpty) {
      sb.append(" ")
      sb.append(
        keyValues.keys.toList.sorted
          .map { k =>
            s"$k: ${keyValues(k)}"
          }
          .mkString(", ")
      )
    }
    sb.toString()
  }
}
