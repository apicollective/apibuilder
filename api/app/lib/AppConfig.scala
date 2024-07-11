package lib

import javax.inject.{Inject, Singleton}

case class RollbarConfig(enabled: Boolean, accessToken: String)
case class SendgridConfig(apiKey: String)

@Singleton
class AppConfig @Inject() (
  config: Config
) {

  val apibuilderWwwHost: String = config.requiredString("apibuilder.app.host")

  val subjectPrefix: String = config.requiredString("mail.subjectPrefix")

  val emailDefaultFromEmail: String = config.requiredString("mail.defaultFromEmail")
  val emailDefaultFromName: String = config.requiredString("mail.defaultFromName")

  val mailLocalDeliveryDir: Option[String] = config.optionalString("mail.localDeliveryDir")

  val sendErrorsTo: Seq[String] = config.requiredString("apibuilder.sendErrorsTo").split("\\s+").toSeq.distinct

  /**
    * optional as only used in production environment
    */
  val sendgridConfig: Option[SendgridConfig] = config.optionalString("sendgrid.apiKey").map { key =>
    SendgridConfig(apiKey = key)
  }

  val rollbarConfig: RollbarConfig = {
    val enabled = config.optionalBoolean("rollbar.enabled").getOrElse(false)
    RollbarConfig(
      enabled = enabled,
      accessToken = config.optionalString("rollbar.access.token").getOrElse {
        assert(!enabled, "rollbar.access.token is required when rollbar is enabled")
        "development"
      }
    )
  }
}
