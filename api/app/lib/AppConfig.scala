package lib

import javax.inject.{Inject, Singleton}

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
  val sendgridApiKey: Option[String] = config.optionalString("sendgrid.apiKey")
}
