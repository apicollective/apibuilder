/**
 * Generated by API Builder - https://www.apibuilder.io
 * Service version: 0.16.53
 * User agent: apibuilder localhost 9000/apicollective/apibuilder-task/latest/play_2_9_scala_3_client
 */
package io.apibuilder.task.v0.models {

  sealed trait EmailData extends _root_.scala.Product with _root_.scala.Serializable {
    def emailDataDiscriminator: EmailDataDiscriminator

  }

  /**
   * Defines the valid discriminator values for the type EmailData
   */
  sealed trait EmailDataDiscriminator extends _root_.scala.Product with _root_.scala.Serializable

  object EmailDataDiscriminator {

    case object EmailDataApplicationCreated extends EmailDataDiscriminator { override def toString = "email_data_application_created" }
    case object EmailDataEmailVerificationCreated extends EmailDataDiscriminator { override def toString = "email_data_email_verification_created" }
    case object EmailDataMembershipCreated extends EmailDataDiscriminator { override def toString = "email_data_membership_created" }
    case object EmailDataMembershipRequestCreated extends EmailDataDiscriminator { override def toString = "email_data_membership_request_created" }
    case object EmailDataMembershipRequestAccepted extends EmailDataDiscriminator { override def toString = "email_data_membership_request_accepted" }
    case object EmailDataMembershipRequestDeclined extends EmailDataDiscriminator { override def toString = "email_data_membership_request_declined" }
    case object EmailDataPasswordResetRequestCreated extends EmailDataDiscriminator { override def toString = "email_data_password_reset_request_created" }

    final case class UNDEFINED(override val toString: String) extends EmailDataDiscriminator

    val all: scala.List[EmailDataDiscriminator] = scala.List(EmailDataApplicationCreated, EmailDataEmailVerificationCreated, EmailDataMembershipCreated, EmailDataMembershipRequestCreated, EmailDataMembershipRequestAccepted, EmailDataMembershipRequestDeclined, EmailDataPasswordResetRequestCreated)

    private val byName: Map[String, EmailDataDiscriminator] = all.map(x => x.toString.toLowerCase -> x).toMap

    def apply(value: String): EmailDataDiscriminator = fromString(value).getOrElse(UNDEFINED(value))

    def fromString(value: String): _root_.scala.Option[EmailDataDiscriminator] = byName.get(value.toLowerCase)

  }
  final case class DiffVersionData(
    oldVersionGuid: _root_.java.util.UUID,
    newVersionGuid: _root_.java.util.UUID
  )

  final case class EmailDataApplicationCreated(
    applicationGuid: _root_.java.util.UUID
  ) extends EmailData {
    override val emailDataDiscriminator: EmailDataDiscriminator = EmailDataDiscriminator.EmailDataApplicationCreated
  }

  final case class EmailDataEmailVerificationCreated(
    guid: _root_.java.util.UUID
  ) extends EmailData {
    override val emailDataDiscriminator: EmailDataDiscriminator = EmailDataDiscriminator.EmailDataEmailVerificationCreated
  }

  final case class EmailDataMembershipCreated(
    guid: _root_.java.util.UUID
  ) extends EmailData {
    override val emailDataDiscriminator: EmailDataDiscriminator = EmailDataDiscriminator.EmailDataMembershipCreated
  }

  final case class EmailDataMembershipRequestAccepted(
    organizationGuid: _root_.java.util.UUID,
    userGuid: _root_.java.util.UUID,
    role: io.apibuilder.common.v0.models.MembershipRole
  ) extends EmailData {
    override val emailDataDiscriminator: EmailDataDiscriminator = EmailDataDiscriminator.EmailDataMembershipRequestAccepted
  }

  final case class EmailDataMembershipRequestCreated(
    guid: _root_.java.util.UUID
  ) extends EmailData {
    override val emailDataDiscriminator: EmailDataDiscriminator = EmailDataDiscriminator.EmailDataMembershipRequestCreated
  }

  final case class EmailDataMembershipRequestDeclined(
    organizationGuid: _root_.java.util.UUID,
    userGuid: _root_.java.util.UUID
  ) extends EmailData {
    override val emailDataDiscriminator: EmailDataDiscriminator = EmailDataDiscriminator.EmailDataMembershipRequestDeclined
  }

  final case class EmailDataPasswordResetRequestCreated(
    guid: _root_.java.util.UUID
  ) extends EmailData {
    override val emailDataDiscriminator: EmailDataDiscriminator = EmailDataDiscriminator.EmailDataPasswordResetRequestCreated
  }

  /**
   * Provides future compatibility in clients - in the future, when a type is added
   * to the union EmailData, it will need to be handled in the client code. This
   * implementation will deserialize these future types as an instance of this class.
   *
   * @param description Information about the type that we received that is undefined in this version of
   *        the client.
   */

  final case class EmailDataUndefinedType(
    description: String
  ) extends EmailData {
    override val emailDataDiscriminator: EmailDataDiscriminator = EmailDataDiscriminator.UNDEFINED(description)
  }
  sealed trait TaskType extends _root_.scala.Product with _root_.scala.Serializable

  object TaskType {

    case object Email extends TaskType { override def toString = "email" }
    case object CheckInvariants extends TaskType { override def toString = "check_invariants" }
    case object IndexApplication extends TaskType { override def toString = "index_application" }
    case object ScheduleMigrateVersions extends TaskType { override def toString = "schedule_migrate_versions" }
    case object MigrateVersion extends TaskType { override def toString = "migrate_version" }
    case object PurgeDeleted extends TaskType { override def toString = "purge_deleted" }
    case object ScheduleSyncGeneratorServices extends TaskType {
      override def toString = "schedule_sync_generator_services"
    }
    case object SyncGeneratorService extends TaskType { override def toString = "sync_generator_service" }
    case object DiffVersion extends TaskType { override def toString = "diff_version" }
    case object UserCreated extends TaskType { override def toString = "user_created" }
    /**
     * UNDEFINED captures values that are sent either in error or
     * that were added by the server after this library was
     * generated. We want to make it easy and obvious for users of
     * this library to handle this case gracefully.
     *
     * We use all CAPS for the variable name to avoid collisions
     * with the camel cased values above.
     */
    final case class UNDEFINED(override val toString: String) extends TaskType

    /**
     * all returns a list of all the valid, known values. We use
     * lower case to avoid collisions with the camel cased values
     * above.
     */
    val all: scala.List[TaskType] = scala.List(Email, CheckInvariants, IndexApplication, ScheduleMigrateVersions, MigrateVersion, PurgeDeleted, ScheduleSyncGeneratorServices, SyncGeneratorService, DiffVersion, UserCreated)

    private
    val byName: Map[String, TaskType] = all.map(x => x.toString.toLowerCase -> x).toMap

    def apply(value: String): TaskType = fromString(value).getOrElse(UNDEFINED(value))

    def fromString(value: String): _root_.scala.Option[TaskType] = byName.get(value.toLowerCase)

  }

}

package io.apibuilder.task.v0.models {

  package object json {
    import play.api.libs.json.__
    import play.api.libs.json.JsString
    import play.api.libs.json.Writes
    import play.api.libs.functional.syntax._
    import io.apibuilder.common.v0.models.json._
    import io.apibuilder.task.v0.models.json._

    private[v0] implicit val jsonReadsUUID: play.api.libs.json.Reads[_root_.java.util.UUID] = __.read[String].map { str =>
      _root_.java.util.UUID.fromString(str)
    }

    private[v0] implicit val jsonWritesUUID: play.api.libs.json.Writes[_root_.java.util.UUID] = (x: _root_.java.util.UUID) => play.api.libs.json.JsString(x.toString)

    private[v0] implicit val jsonReadsJodaDateTime: play.api.libs.json.Reads[_root_.org.joda.time.DateTime] = __.read[String].map { str =>
      _root_.org.joda.time.format.ISODateTimeFormat.dateTimeParser.parseDateTime(str)
    }

    private[v0] implicit val jsonWritesJodaDateTime: play.api.libs.json.Writes[_root_.org.joda.time.DateTime] = (x: _root_.org.joda.time.DateTime) => {
      play.api.libs.json.JsString(_root_.org.joda.time.format.ISODateTimeFormat.dateTime.print(x))
    }

    private[v0] implicit val jsonReadsJodaLocalDate: play.api.libs.json.Reads[_root_.org.joda.time.LocalDate] = __.read[String].map { str =>
      _root_.org.joda.time.format.ISODateTimeFormat.dateTimeParser.parseLocalDate(str)
    }

    private[v0] implicit val jsonWritesJodaLocalDate: play.api.libs.json.Writes[_root_.org.joda.time.LocalDate] = (x: _root_.org.joda.time.LocalDate) => {
      play.api.libs.json.JsString(_root_.org.joda.time.format.ISODateTimeFormat.date.print(x))
    }

    implicit val jsonReadsApibuilderTaskTaskType: play.api.libs.json.Reads[io.apibuilder.task.v0.models.TaskType] = new play.api.libs.json.Reads[io.apibuilder.task.v0.models.TaskType] {
      def reads(js: play.api.libs.json.JsValue): play.api.libs.json.JsResult[io.apibuilder.task.v0.models.TaskType] = {
        js match {
          case v: play.api.libs.json.JsString => play.api.libs.json.JsSuccess(io.apibuilder.task.v0.models.TaskType(v.value))
          case _ => {
            (js \ "value").validate[String] match {
              case play.api.libs.json.JsSuccess(v, _) => play.api.libs.json.JsSuccess(io.apibuilder.task.v0.models.TaskType(v))
              case err: play.api.libs.json.JsError =>
                (js \ "task_type").validate[String] match {
                  case play.api.libs.json.JsSuccess(v, _) => play.api.libs.json.JsSuccess(io.apibuilder.task.v0.models.TaskType(v))
                  case err: play.api.libs.json.JsError => err
                }
            }
          }
        }
      }
    }

    def jsonWritesApibuilderTaskTaskType(obj: io.apibuilder.task.v0.models.TaskType) = {
      play.api.libs.json.JsString(obj.toString)
    }

    def jsObjectTaskType(obj: io.apibuilder.task.v0.models.TaskType) = {
      play.api.libs.json.Json.obj("value" -> play.api.libs.json.JsString(obj.toString))
    }

    implicit def jsonWritesApibuilderTaskTaskType: play.api.libs.json.Writes[TaskType] = {
      (obj: io.apibuilder.task.v0.models.TaskType) => {
        io.apibuilder.task.v0.models.json.jsonWritesApibuilderTaskTaskType(obj)
      }
    }

    implicit def jsonReadsApibuilderTaskDiffVersionData: play.api.libs.json.Reads[io.apibuilder.task.v0.models.DiffVersionData] = {
      for {
        oldVersionGuid <- (__ \ "old_version_guid").read[_root_.java.util.UUID]
        newVersionGuid <- (__ \ "new_version_guid").read[_root_.java.util.UUID]
      } yield DiffVersionData(oldVersionGuid, newVersionGuid)
    }

    def jsObjectDiffVersionData(obj: io.apibuilder.task.v0.models.DiffVersionData): play.api.libs.json.JsObject = {
      play.api.libs.json.Json.obj(
        "old_version_guid" -> play.api.libs.json.JsString(obj.oldVersionGuid.toString),
        "new_version_guid" -> play.api.libs.json.JsString(obj.newVersionGuid.toString)
      )
    }

    implicit def jsonWritesApibuilderTaskDiffVersionData: play.api.libs.json.Writes[DiffVersionData] = {
      (obj: io.apibuilder.task.v0.models.DiffVersionData) => {
        io.apibuilder.task.v0.models.json.jsObjectDiffVersionData(obj)
      }
    }

    def jsonReadsApibuilderTaskEmailDataApplicationCreated: play.api.libs.json.Reads[io.apibuilder.task.v0.models.EmailDataApplicationCreated] = {
      (__ \ "application_guid").read[_root_.java.util.UUID].map { x => EmailDataApplicationCreated(applicationGuid = x) }
    }

    def jsObjectEmailDataApplicationCreated(obj: io.apibuilder.task.v0.models.EmailDataApplicationCreated): play.api.libs.json.JsObject = {
      play.api.libs.json.Json.obj(
        "application_guid" -> play.api.libs.json.JsString(obj.applicationGuid.toString)
      ) ++ play.api.libs.json.Json.obj("discriminator" -> "email_data_application_created")
    }

    def jsonWritesApibuilderTaskEmailDataApplicationCreated: play.api.libs.json.Writes[EmailDataApplicationCreated] = {
      (obj: io.apibuilder.task.v0.models.EmailDataApplicationCreated) => {
        io.apibuilder.task.v0.models.json.jsObjectEmailDataApplicationCreated(obj)
      }
    }

    def jsonReadsApibuilderTaskEmailDataEmailVerificationCreated: play.api.libs.json.Reads[io.apibuilder.task.v0.models.EmailDataEmailVerificationCreated] = {
      (__ \ "guid").read[_root_.java.util.UUID].map { x => EmailDataEmailVerificationCreated(guid = x) }
    }

    def jsObjectEmailDataEmailVerificationCreated(obj: io.apibuilder.task.v0.models.EmailDataEmailVerificationCreated): play.api.libs.json.JsObject = {
      play.api.libs.json.Json.obj(
        "guid" -> play.api.libs.json.JsString(obj.guid.toString)
      ) ++ play.api.libs.json.Json.obj("discriminator" -> "email_data_email_verification_created")
    }

    def jsonWritesApibuilderTaskEmailDataEmailVerificationCreated: play.api.libs.json.Writes[EmailDataEmailVerificationCreated] = {
      (obj: io.apibuilder.task.v0.models.EmailDataEmailVerificationCreated) => {
        io.apibuilder.task.v0.models.json.jsObjectEmailDataEmailVerificationCreated(obj)
      }
    }

    def jsonReadsApibuilderTaskEmailDataMembershipCreated: play.api.libs.json.Reads[io.apibuilder.task.v0.models.EmailDataMembershipCreated] = {
      (__ \ "guid").read[_root_.java.util.UUID].map { x => EmailDataMembershipCreated(guid = x) }
    }

    def jsObjectEmailDataMembershipCreated(obj: io.apibuilder.task.v0.models.EmailDataMembershipCreated): play.api.libs.json.JsObject = {
      play.api.libs.json.Json.obj(
        "guid" -> play.api.libs.json.JsString(obj.guid.toString)
      ) ++ play.api.libs.json.Json.obj("discriminator" -> "email_data_membership_created")
    }

    def jsonWritesApibuilderTaskEmailDataMembershipCreated: play.api.libs.json.Writes[EmailDataMembershipCreated] = {
      (obj: io.apibuilder.task.v0.models.EmailDataMembershipCreated) => {
        io.apibuilder.task.v0.models.json.jsObjectEmailDataMembershipCreated(obj)
      }
    }

    def jsonReadsApibuilderTaskEmailDataMembershipRequestAccepted: play.api.libs.json.Reads[io.apibuilder.task.v0.models.EmailDataMembershipRequestAccepted] = {
      for {
        organizationGuid <- (__ \ "organization_guid").read[_root_.java.util.UUID]
        userGuid <- (__ \ "user_guid").read[_root_.java.util.UUID]
        role <- (__ \ "role").read[io.apibuilder.common.v0.models.MembershipRole]
      } yield EmailDataMembershipRequestAccepted(organizationGuid, userGuid, role)
    }

    def jsObjectEmailDataMembershipRequestAccepted(obj: io.apibuilder.task.v0.models.EmailDataMembershipRequestAccepted): play.api.libs.json.JsObject = {
      play.api.libs.json.Json.obj(
        "organization_guid" -> play.api.libs.json.JsString(obj.organizationGuid.toString),
        "user_guid" -> play.api.libs.json.JsString(obj.userGuid.toString),
        "role" -> play.api.libs.json.JsString(obj.role.toString)
      ) ++ play.api.libs.json.Json.obj("discriminator" -> "email_data_membership_request_accepted")
    }

    def jsonWritesApibuilderTaskEmailDataMembershipRequestAccepted: play.api.libs.json.Writes[EmailDataMembershipRequestAccepted] = {
      (obj: io.apibuilder.task.v0.models.EmailDataMembershipRequestAccepted) => {
        io.apibuilder.task.v0.models.json.jsObjectEmailDataMembershipRequestAccepted(obj)
      }
    }

    def jsonReadsApibuilderTaskEmailDataMembershipRequestCreated: play.api.libs.json.Reads[io.apibuilder.task.v0.models.EmailDataMembershipRequestCreated] = {
      (__ \ "guid").read[_root_.java.util.UUID].map { x => EmailDataMembershipRequestCreated(guid = x) }
    }

    def jsObjectEmailDataMembershipRequestCreated(obj: io.apibuilder.task.v0.models.EmailDataMembershipRequestCreated): play.api.libs.json.JsObject = {
      play.api.libs.json.Json.obj(
        "guid" -> play.api.libs.json.JsString(obj.guid.toString)
      ) ++ play.api.libs.json.Json.obj("discriminator" -> "email_data_membership_request_created")
    }

    def jsonWritesApibuilderTaskEmailDataMembershipRequestCreated: play.api.libs.json.Writes[EmailDataMembershipRequestCreated] = {
      (obj: io.apibuilder.task.v0.models.EmailDataMembershipRequestCreated) => {
        io.apibuilder.task.v0.models.json.jsObjectEmailDataMembershipRequestCreated(obj)
      }
    }

    def jsonReadsApibuilderTaskEmailDataMembershipRequestDeclined: play.api.libs.json.Reads[io.apibuilder.task.v0.models.EmailDataMembershipRequestDeclined] = {
      for {
        organizationGuid <- (__ \ "organization_guid").read[_root_.java.util.UUID]
        userGuid <- (__ \ "user_guid").read[_root_.java.util.UUID]
      } yield EmailDataMembershipRequestDeclined(organizationGuid, userGuid)
    }

    def jsObjectEmailDataMembershipRequestDeclined(obj: io.apibuilder.task.v0.models.EmailDataMembershipRequestDeclined): play.api.libs.json.JsObject = {
      play.api.libs.json.Json.obj(
        "organization_guid" -> play.api.libs.json.JsString(obj.organizationGuid.toString),
        "user_guid" -> play.api.libs.json.JsString(obj.userGuid.toString)
      ) ++ play.api.libs.json.Json.obj("discriminator" -> "email_data_membership_request_declined")
    }

    def jsonWritesApibuilderTaskEmailDataMembershipRequestDeclined: play.api.libs.json.Writes[EmailDataMembershipRequestDeclined] = {
      (obj: io.apibuilder.task.v0.models.EmailDataMembershipRequestDeclined) => {
        io.apibuilder.task.v0.models.json.jsObjectEmailDataMembershipRequestDeclined(obj)
      }
    }

    def jsonReadsApibuilderTaskEmailDataPasswordResetRequestCreated: play.api.libs.json.Reads[io.apibuilder.task.v0.models.EmailDataPasswordResetRequestCreated] = {
      (__ \ "guid").read[_root_.java.util.UUID].map { x => EmailDataPasswordResetRequestCreated(guid = x) }
    }

    def jsObjectEmailDataPasswordResetRequestCreated(obj: io.apibuilder.task.v0.models.EmailDataPasswordResetRequestCreated): play.api.libs.json.JsObject = {
      play.api.libs.json.Json.obj(
        "guid" -> play.api.libs.json.JsString(obj.guid.toString)
      ) ++ play.api.libs.json.Json.obj("discriminator" -> "email_data_password_reset_request_created")
    }

    def jsonWritesApibuilderTaskEmailDataPasswordResetRequestCreated: play.api.libs.json.Writes[EmailDataPasswordResetRequestCreated] = {
      (obj: io.apibuilder.task.v0.models.EmailDataPasswordResetRequestCreated) => {
        io.apibuilder.task.v0.models.json.jsObjectEmailDataPasswordResetRequestCreated(obj)
      }
    }

    implicit def jsonReadsApibuilderTaskEmailData[T <: io.apibuilder.task.v0.models.EmailData]: play.api.libs.json.Reads[T] = (js: play.api.libs.json.JsValue) => {
      def readDiscriminator(discriminator: String) = {
        discriminator match {
          case "email_data_application_created" => io.apibuilder.task.v0.models.json.jsonReadsApibuilderTaskEmailDataApplicationCreated.reads(js)
          case "email_data_email_verification_created" => io.apibuilder.task.v0.models.json.jsonReadsApibuilderTaskEmailDataEmailVerificationCreated.reads(js)
          case "email_data_membership_created" => io.apibuilder.task.v0.models.json.jsonReadsApibuilderTaskEmailDataMembershipCreated.reads(js)
          case "email_data_membership_request_created" => io.apibuilder.task.v0.models.json.jsonReadsApibuilderTaskEmailDataMembershipRequestCreated.reads(js)
          case "email_data_membership_request_accepted" => io.apibuilder.task.v0.models.json.jsonReadsApibuilderTaskEmailDataMembershipRequestAccepted.reads(js)
          case "email_data_membership_request_declined" => io.apibuilder.task.v0.models.json.jsonReadsApibuilderTaskEmailDataMembershipRequestDeclined.reads(js)
          case "email_data_password_reset_request_created" => io.apibuilder.task.v0.models.json.jsonReadsApibuilderTaskEmailDataPasswordResetRequestCreated.reads(js)
          case other => play.api.libs.json.JsSuccess(io.apibuilder.task.v0.models.EmailDataUndefinedType(other))
        }
      }
      (js \ "discriminator").validate[String] match {
        case e: play.api.libs.json.JsError => e
        case s: play.api.libs.json.JsSuccess[String] => readDiscriminator(s.value).map(_.asInstanceOf[T])
      }
    }


    implicit def jsonReadsApibuilderTaskEmailDataSeq[T <: io.apibuilder.task.v0.models.EmailData]: play.api.libs.json.Reads[Seq[T]] = {
      case a: play.api.libs.json.JsArray => {
        val all: Seq[play.api.libs.json.JsResult[io.apibuilder.task.v0.models.EmailData]] = a.value.map(jsonReadsApibuilderTaskEmailData.reads).toSeq

        all.collect { case e: play.api.libs.json.JsError => e }.toList match {
          case Nil => play.api.libs.json.JsSuccess(all.collect { case play.api.libs.json.JsSuccess(v, _) => v.asInstanceOf[T] })
          case errors => play.api.libs.json.JsError(play.api.libs.json.JsError.merge(errors.flatMap(_.errors), Nil))
        }
      }
      case other => play.api.libs.json.JsError(s"Expected array but found [" + other.getClass.getName + "]")
    }

    def jsObjectEmailData(obj: io.apibuilder.task.v0.models.EmailData): play.api.libs.json.JsObject = {
      obj match {
        case x: io.apibuilder.task.v0.models.EmailDataApplicationCreated => io.apibuilder.task.v0.models.json.jsObjectEmailDataApplicationCreated(x)
        case x: io.apibuilder.task.v0.models.EmailDataEmailVerificationCreated => io.apibuilder.task.v0.models.json.jsObjectEmailDataEmailVerificationCreated(x)
        case x: io.apibuilder.task.v0.models.EmailDataMembershipCreated => io.apibuilder.task.v0.models.json.jsObjectEmailDataMembershipCreated(x)
        case x: io.apibuilder.task.v0.models.EmailDataMembershipRequestCreated => io.apibuilder.task.v0.models.json.jsObjectEmailDataMembershipRequestCreated(x)
        case x: io.apibuilder.task.v0.models.EmailDataMembershipRequestAccepted => io.apibuilder.task.v0.models.json.jsObjectEmailDataMembershipRequestAccepted(x)
        case x: io.apibuilder.task.v0.models.EmailDataMembershipRequestDeclined => io.apibuilder.task.v0.models.json.jsObjectEmailDataMembershipRequestDeclined(x)
        case x: io.apibuilder.task.v0.models.EmailDataPasswordResetRequestCreated => io.apibuilder.task.v0.models.json.jsObjectEmailDataPasswordResetRequestCreated(x)
        case other => {
          sys.error(s"The type[${other.getClass.getName}] has no JSON writer")
        }
      }
    }

    implicit def jsonWritesApibuilderTaskEmailData[T <: io.apibuilder.task.v0.models.EmailData]: play.api.libs.json.Writes[T] = {
      (obj: io.apibuilder.task.v0.models.EmailData) => {
        io.apibuilder.task.v0.models.json.jsObjectEmailData(obj)
      }
    }
  }
}

package io.apibuilder.task.v0 {

  object Bindables {

    import play.api.mvc.{PathBindable, QueryStringBindable}

    // import models directly for backwards compatibility with prior versions of the generator
    import Core._
    import Models._

    object Core {
      implicit def pathBindableDateTimeIso8601(implicit stringBinder: QueryStringBindable[String]): PathBindable[_root_.org.joda.time.DateTime] = ApibuilderPathBindable(ApibuilderTypes.dateTimeIso8601)
      implicit def queryStringBindableDateTimeIso8601(implicit stringBinder: QueryStringBindable[String]): QueryStringBindable[_root_.org.joda.time.DateTime] = ApibuilderQueryStringBindable(ApibuilderTypes.dateTimeIso8601)

      implicit def pathBindableDateIso8601(implicit stringBinder: QueryStringBindable[String]): PathBindable[_root_.org.joda.time.LocalDate] = ApibuilderPathBindable(ApibuilderTypes.dateIso8601)
      implicit def queryStringBindableDateIso8601(implicit stringBinder: QueryStringBindable[String]): QueryStringBindable[_root_.org.joda.time.LocalDate] = ApibuilderQueryStringBindable(ApibuilderTypes.dateIso8601)
    }

    object Models {
      import io.apibuilder.task.v0.models._

      val taskTypeConverter: ApibuilderTypeConverter[io.apibuilder.task.v0.models.TaskType] = new ApibuilderTypeConverter[io.apibuilder.task.v0.models.TaskType] {
        override def convert(value: String): io.apibuilder.task.v0.models.TaskType = io.apibuilder.task.v0.models.TaskType(value)
        override def convert(value: io.apibuilder.task.v0.models.TaskType): String = value.toString
        override def example: io.apibuilder.task.v0.models.TaskType = io.apibuilder.task.v0.models.TaskType.Email
        override def validValues: Seq[io.apibuilder.task.v0.models.TaskType] = io.apibuilder.task.v0.models.TaskType.all
      }
      implicit def pathBindableTaskType(implicit stringBinder: QueryStringBindable[String]): PathBindable[io.apibuilder.task.v0.models.TaskType] = ApibuilderPathBindable(taskTypeConverter)
      implicit def queryStringBindableTaskType(implicit stringBinder: QueryStringBindable[String]): QueryStringBindable[io.apibuilder.task.v0.models.TaskType] = ApibuilderQueryStringBindable(taskTypeConverter)
    }

    trait ApibuilderTypeConverter[T] {

      def convert(value: String): T

      def convert(value: T): String

      def example: T

      def validValues: Seq[T] = Nil

      def errorMessage(key: String, value: String, ex: java.lang.Exception): String = {
        val base = s"Invalid value '$value' for parameter '$key'. "
        validValues.toList match {
          case Nil => base + "Ex: " + convert(example)
          case values => base + ". Valid values are: " + values.mkString("'", "', '", "'")
        }
      }
    }

    object ApibuilderTypes {
      val dateTimeIso8601: ApibuilderTypeConverter[_root_.org.joda.time.DateTime] = new ApibuilderTypeConverter[_root_.org.joda.time.DateTime] {
        override def convert(value: String): _root_.org.joda.time.DateTime = _root_.org.joda.time.format.ISODateTimeFormat.dateTimeParser.parseDateTime(value)
        override def convert(value: _root_.org.joda.time.DateTime): String = _root_.org.joda.time.format.ISODateTimeFormat.dateTime.print(value)
        override def example: _root_.org.joda.time.DateTime = _root_.org.joda.time.DateTime.now
      }

      val dateIso8601: ApibuilderTypeConverter[_root_.org.joda.time.LocalDate] = new ApibuilderTypeConverter[_root_.org.joda.time.LocalDate] {
        override def convert(value: String): _root_.org.joda.time.LocalDate = _root_.org.joda.time.format.ISODateTimeFormat.dateTimeParser.parseLocalDate(value)
        override def convert(value: _root_.org.joda.time.LocalDate): String = _root_.org.joda.time.format.ISODateTimeFormat.date.print(value)
        override def example: _root_.org.joda.time.LocalDate = _root_.org.joda.time.LocalDate.now
      }
    }

    final case class ApibuilderQueryStringBindable[T](
      converters: ApibuilderTypeConverter[T]
    ) extends QueryStringBindable[T] {

      override def bind(key: String, params: Map[String, Seq[String]]): _root_.scala.Option[_root_.scala.Either[String, T]] = {
        params.getOrElse(key, Nil).headOption.map { v =>
          try {
            Right(
              converters.convert(v)
            )
          } catch {
            case ex: java.lang.Exception => Left(
              converters.errorMessage(key, v, ex)
            )
          }
        }
      }

      override def unbind(key: String, value: T): String = {
        s"$key=${converters.convert(value)}"
      }
    }

    final case class ApibuilderPathBindable[T](
      converters: ApibuilderTypeConverter[T]
    ) extends PathBindable[T] {

      override def bind(key: String, value: String): _root_.scala.Either[String, T] = {
        try {
          Right(
            converters.convert(value)
          )
        } catch {
          case ex: java.lang.Exception => Left(
            converters.errorMessage(key, value, ex)
          )
        }
      }

      override def unbind(key: String, value: T): String = {
        converters.convert(value)
      }
    }

  }

}


package io.apibuilder.task.v0 {

  object Constants {

    val Namespace = "io.apibuilder.task.v0"
    val UserAgent = "apibuilder localhost 9000/apicollective/apibuilder-task/latest/play_2_9_scala_3_client"
    val Version = "0.16.53"
    val VersionMajor = 0

  }

  class Client(
    ws: play.api.libs.ws.WSClient,
    val baseUrl: String,
    auth: scala.Option[io.apibuilder.task.v0.Authorization] = None,
    defaultHeaders: Seq[(String, String)] = Nil
  ) extends interfaces.Client {
    import io.apibuilder.common.v0.models.json._
    import io.apibuilder.task.v0.models.json._

    private val logger = play.api.Logger("io.apibuilder.task.v0.Client")

    logger.info(s"Initializing io.apibuilder.task.v0.Client for url $baseUrl")





    def _requestHolder(path: String): play.api.libs.ws.WSRequest = {

      val holder = ws.url(baseUrl + path).addHttpHeaders(
        "User-Agent" -> Constants.UserAgent,
        "X-Apidoc-Version" -> Constants.Version,
        "X-Apidoc-Version-Major" -> Constants.VersionMajor.toString
      ).addHttpHeaders(defaultHeaders*)
      auth.fold(holder) {
        case Authorization.Basic(username, password) => {
          holder.withAuth(username, password.getOrElse(""), play.api.libs.ws.WSAuthScheme.BASIC)
        }
      }
    }

    def _logRequest(method: String, req: play.api.libs.ws.WSRequest): play.api.libs.ws.WSRequest = {
      val queryComponents = for {
        (name, values) <- req.queryString
        value <- values
      } yield s"$name=$value"
      val url = s"${req.url}${queryComponents.mkString("?", "&", "")}"
      auth.fold(logger.info(s"curl -X $method '$url'")) { _ =>
        logger.info(s"curl -X $method -u '[REDACTED]:' '$url'")
      }
      req
    }

    def _executeRequest(
      method: String,
      path: String,
      queryParameters: Seq[(String, String)] = Nil,
      requestHeaders: Seq[(String, String)] = Nil,
      body: Option[play.api.libs.json.JsValue] = None
    ): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue

      method.toUpperCase match {
        case "GET" => {
          _logRequest("GET", _requestHolder(path).addHttpHeaders(requestHeaders*).addQueryStringParameters(queryParameters*)).get()
        }
        case "POST" => {
          _logRequest("POST", _requestHolder(path).addHttpHeaders(_withJsonContentType(requestHeaders)*).addQueryStringParameters(queryParameters*)).post(body.getOrElse(play.api.libs.json.Json.obj()))
        }
        case "PUT" => {
          _logRequest("PUT", _requestHolder(path).addHttpHeaders(_withJsonContentType(requestHeaders)*).addQueryStringParameters(queryParameters*)).put(body.getOrElse(play.api.libs.json.Json.obj()))
        }
        case "PATCH" => {
          _logRequest("PATCH", _requestHolder(path).addHttpHeaders(requestHeaders*).addQueryStringParameters(queryParameters*)).patch(body.getOrElse(play.api.libs.json.Json.obj()))
        }
        case "DELETE" => {
          _logRequest("DELETE", _requestHolder(path).addHttpHeaders(requestHeaders*).addQueryStringParameters(queryParameters*)).delete()
        }
         case "HEAD" => {
          _logRequest("HEAD", _requestHolder(path).addHttpHeaders(requestHeaders*).addQueryStringParameters(queryParameters*)).head()
        }
         case "OPTIONS" => {
          _logRequest("OPTIONS", _requestHolder(path).addHttpHeaders(requestHeaders*).addQueryStringParameters(queryParameters*)).options()
        }
        case _ => {
          _logRequest(method, _requestHolder(path).addHttpHeaders(requestHeaders*).addQueryStringParameters(queryParameters*))
          sys.error("Unsupported method[%s]".format(method))
        }
      }
    }

    /**
     * Adds a Content-Type: application/json header unless the specified requestHeaders
     * already contain a Content-Type header
     */
    def _withJsonContentType(headers: Seq[(String, String)]): Seq[(String, String)] = {
      headers.find { _._1.toUpperCase == "CONTENT-TYPE" } match {
        case None => headers ++ Seq("Content-Type" -> "application/json; charset=UTF-8")
        case Some(_) => headers
      }
    }

  }

  object Client {

    def parseJson[T](
      className: String,
      r: play.api.libs.ws.WSResponse,
      f: (play.api.libs.json.JsValue => play.api.libs.json.JsResult[T])
    ): T = {
      f(play.api.libs.json.Json.parse(r.body)) match {
        case play.api.libs.json.JsSuccess(x, _) => x
        case play.api.libs.json.JsError(errors) => {
          throw io.apibuilder.task.v0.errors.FailedRequest(r.status, s"Invalid json for class[" + className + "]: " + errors.mkString(" "))
        }
      }
    }

  }

  sealed trait Authorization extends _root_.scala.Product with _root_.scala.Serializable
  object Authorization {
    final case class Basic(username: String, password: Option[String] = None) extends Authorization
  }

  package interfaces {

    trait Client {
      def baseUrl: String

    }

  }



  package errors {

    final case class FailedRequest(responseCode: Int, message: String, requestUri: Option[_root_.java.net.URI] = None) extends _root_.java.lang.Exception(s"HTTP $responseCode: $message")

  }

}