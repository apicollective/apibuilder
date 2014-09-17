package com.gilt.quality.models {
  case class Error(
    code: String,
    message: String
  )

  /**
   * Represents something that has happened - e.g. a team was created, an incident
   * created, a plan updated, etc.
   */
  case class Event(
    model: Model,
    action: Action,
    timestamp: org.joda.time.DateTime,
    url: scala.Option[String] = None,
    data: EventData
  )

  /**
   * Generic, descriptive data about a specific event
   */
  case class EventData(
    modelId: Long,
    summary: String
  )

  case class Healthcheck(
    status: String
  )

  /**
   * A bug or error that affected public or internal users in a negative way
   */
  case class Incident(
    id: Long,
    summary: String,
    description: scala.Option[String] = None,
    team: scala.Option[Team] = None,
    severity: Severity,
    tags: scala.collection.Seq[String] = Nil,
    plan: scala.Option[Plan] = None,
    createdAt: org.joda.time.DateTime
  )

  /**
   * Details for how an incident will be resolved
   */
  case class Plan(
    id: Long,
    incidentId: Long,
    body: String,
    grade: scala.Option[Int] = None,
    createdAt: org.joda.time.DateTime
  )

  /**
   * Statistics on each team's quality metrics, number of issues
   */
  case class Statistic(
    team: Team,
    totalGrades: Long,
    averageGrade: scala.Option[Int] = None,
    totalOpenIncidents: Long,
    totalIncidents: Long,
    totalPlans: Long,
    plans: scala.collection.Seq[Plan] = Nil
  )

  /**
   * A team is the main actor in the system. Teams have a unique key and own
   * incidents
   */
  case class Team(
    key: String
  )

  /**
   * Used in the event system to indicate what happened.
   */
  sealed trait Action

  object Action {

    /**
     * Indicates that an instance of this model was created
     */
    case object Created extends Action { override def toString = "created" }
    /**
     * Indicates that an instance of this model was updated
     */
    case object Updated extends Action { override def toString = "updated" }
    /**
     * Indicates that an instance of this model was deleted
     */
    case object Deleted extends Action { override def toString = "deleted" }

    /**
     * UNDEFINED captures values that are sent either in error or
     * that were added by the server after this library was
     * generated. We want to make it easy and obvious for users of
     * this library to handle this case gracefully.
     *
     * We use all CAPS for the variable name to avoid collisions
     * with the camel cased values above.
     */
    case class UNDEFINED(override val toString: String) extends Action

    /**
     * all returns a list of all the valid, known values. We use
     * lower case to avoid collisions with the camel cased values
     * above.
     */
    val all = Seq(Created, Updated, Deleted)

    private[this]
    val byName = all.map(x => x.toString -> x).toMap

    def apply(value: String): Action = fromString(value).getOrElse(UNDEFINED(value))

    def fromString(value: String): scala.Option[Action] = byName.get(value)

  }

  /**
   * The name of the model that was the subject of the event
   */
  sealed trait Model

  object Model {

    case object Incident extends Model { override def toString = "incident" }
    case object Plan extends Model { override def toString = "plan" }
    case object Rating extends Model { override def toString = "rating" }

    /**
     * UNDEFINED captures values that are sent either in error or
     * that were added by the server after this library was
     * generated. We want to make it easy and obvious for users of
     * this library to handle this case gracefully.
     *
     * We use all CAPS for the variable name to avoid collisions
     * with the camel cased values above.
     */
    case class UNDEFINED(override val toString: String) extends Model

    /**
     * all returns a list of all the valid, known values. We use
     * lower case to avoid collisions with the camel cased values
     * above.
     */
    val all = Seq(Incident, Plan, Rating)

    private[this]
    val byName = all.map(x => x.toString -> x).toMap

    def apply(value: String): Model = fromString(value).getOrElse(UNDEFINED(value))

    def fromString(value: String): scala.Option[Model] = byName.get(value)

  }

  sealed trait Severity

  object Severity {

    case object Low extends Severity { override def toString = "low" }
    case object High extends Severity { override def toString = "high" }

    /**
     * UNDEFINED captures values that are sent either in error or
     * that were added by the server after this library was
     * generated. We want to make it easy and obvious for users of
     * this library to handle this case gracefully.
     *
     * We use all CAPS for the variable name to avoid collisions
     * with the camel cased values above.
     */
    case class UNDEFINED(override val toString: String) extends Severity

    /**
     * all returns a list of all the valid, known values. We use
     * lower case to avoid collisions with the camel cased values
     * above.
     */
    val all = Seq(Low, High)

    private[this]
    val byName = all.map(x => x.toString -> x).toMap

    def apply(value: String): Severity = fromString(value).getOrElse(UNDEFINED(value))

    def fromString(value: String): scala.Option[Severity] = byName.get(value)

  }
}

package com.gilt.quality.models {
  package object json {
    import play.api.libs.json.__
    import play.api.libs.json.JsString
    import play.api.libs.json.Writes
    import play.api.libs.functional.syntax._

    private[quality] implicit val jsonReadsUUID = __.read[String].map(java.util.UUID.fromString)

    private[quality] implicit val jsonWritesUUID = new Writes[java.util.UUID] {
      def writes(x: java.util.UUID) = JsString(x.toString)
    }

    private[quality] implicit val jsonReadsJodaDateTime = __.read[String].map { str =>
      import org.joda.time.format.ISODateTimeFormat.dateTimeParser
      dateTimeParser.parseDateTime(str)
    }

    private[quality] implicit val jsonWritesJodaDateTime = new Writes[org.joda.time.DateTime] {
      def writes(x: org.joda.time.DateTime) = {
        import org.joda.time.format.ISODateTimeFormat.dateTime
        val str = dateTime.print(x)
        JsString(str)
      }
    }

    implicit val jsonReadsQualityEnum_Action = __.read[String].map(Action.apply)
    implicit val jsonWritesQualityEnum_Action = new Writes[Action] {
      def writes(x: Action) = JsString(x.toString)
    }

    implicit val jsonReadsQualityEnum_Model = __.read[String].map(Model.apply)
    implicit val jsonWritesQualityEnum_Model = new Writes[Model] {
      def writes(x: Model) = JsString(x.toString)
    }

    implicit val jsonReadsQualityEnum_Severity = __.read[String].map(Severity.apply)
    implicit val jsonWritesQualityEnum_Severity = new Writes[Severity] {
      def writes(x: Severity) = JsString(x.toString)
    }
    implicit def jsonReadsQualityError: play.api.libs.json.Reads[Error] = {
      (
        (__ \ "code").read[String] and
        (__ \ "message").read[String]
      )(Error.apply _)
    }

    implicit def jsonWritesQualityError: play.api.libs.json.Writes[Error] = {
      (
        (__ \ "code").write[String] and
        (__ \ "message").write[String]
      )(unlift(Error.unapply _))
    }

    implicit def jsonReadsQualityEvent: play.api.libs.json.Reads[Event] = {
      (
        (__ \ "model").read[Model] and
        (__ \ "action").read[Action] and
        (__ \ "timestamp").read[org.joda.time.DateTime] and
        (__ \ "url").readNullable[String] and
        (__ \ "data").read[EventData]
      )(Event.apply _)
    }

    implicit def jsonWritesQualityEvent: play.api.libs.json.Writes[Event] = {
      (
        (__ \ "model").write[Model] and
        (__ \ "action").write[Action] and
        (__ \ "timestamp").write[org.joda.time.DateTime] and
        (__ \ "url").write[scala.Option[String]] and
        (__ \ "data").write[EventData]
      )(unlift(Event.unapply _))
    }

    implicit def jsonReadsQualityEventData: play.api.libs.json.Reads[EventData] = {
      (
        (__ \ "model_id").read[Long] and
        (__ \ "summary").read[String]
      )(EventData.apply _)
    }

    implicit def jsonWritesQualityEventData: play.api.libs.json.Writes[EventData] = {
      (
        (__ \ "model_id").write[Long] and
        (__ \ "summary").write[String]
      )(unlift(EventData.unapply _))
    }

    implicit def jsonReadsQualityHealthcheck: play.api.libs.json.Reads[Healthcheck] = {
      (__ \ "status").read[String].map { x => new Healthcheck(status = x) }
    }

    implicit def jsonWritesQualityHealthcheck: play.api.libs.json.Writes[Healthcheck] = new play.api.libs.json.Writes[Healthcheck] {
      def writes(x: Healthcheck) = play.api.libs.json.Json.obj(
        "status" -> play.api.libs.json.Json.toJson(x.status)
      )
    }

    implicit def jsonReadsQualityIncident: play.api.libs.json.Reads[Incident] = {
      (
        (__ \ "id").read[Long] and
        (__ \ "summary").read[String] and
        (__ \ "description").readNullable[String] and
        (__ \ "team").readNullable[Team] and
        (__ \ "severity").read[Severity] and
        (__ \ "tags").readNullable[scala.collection.Seq[String]].map(_.getOrElse(Nil)) and
        (__ \ "plan").readNullable[Plan] and
        (__ \ "created_at").read[org.joda.time.DateTime]
      )(Incident.apply _)
    }

    implicit def jsonWritesQualityIncident: play.api.libs.json.Writes[Incident] = {
      (
        (__ \ "id").write[Long] and
        (__ \ "summary").write[String] and
        (__ \ "description").write[scala.Option[String]] and
        (__ \ "team").write[scala.Option[Team]] and
        (__ \ "severity").write[Severity] and
        (__ \ "tags").write[scala.collection.Seq[String]] and
        (__ \ "plan").write[scala.Option[Plan]] and
        (__ \ "created_at").write[org.joda.time.DateTime]
      )(unlift(Incident.unapply _))
    }

    implicit def jsonReadsQualityPlan: play.api.libs.json.Reads[Plan] = {
      (
        (__ \ "id").read[Long] and
        (__ \ "incident_id").read[Long] and
        (__ \ "body").read[String] and
        (__ \ "grade").readNullable[Int] and
        (__ \ "created_at").read[org.joda.time.DateTime]
      )(Plan.apply _)
    }

    implicit def jsonWritesQualityPlan: play.api.libs.json.Writes[Plan] = {
      (
        (__ \ "id").write[Long] and
        (__ \ "incident_id").write[Long] and
        (__ \ "body").write[String] and
        (__ \ "grade").write[scala.Option[Int]] and
        (__ \ "created_at").write[org.joda.time.DateTime]
      )(unlift(Plan.unapply _))
    }

    implicit def jsonReadsQualityStatistic: play.api.libs.json.Reads[Statistic] = {
      (
        (__ \ "team").read[Team] and
        (__ \ "total_grades").read[Long] and
        (__ \ "average_grade").readNullable[Int] and
        (__ \ "total_open_incidents").read[Long] and
        (__ \ "total_incidents").read[Long] and
        (__ \ "total_plans").read[Long] and
        (__ \ "plans").readNullable[scala.collection.Seq[Plan]].map(_.getOrElse(Nil))
      )(Statistic.apply _)
    }

    implicit def jsonWritesQualityStatistic: play.api.libs.json.Writes[Statistic] = {
      (
        (__ \ "team").write[Team] and
        (__ \ "total_grades").write[Long] and
        (__ \ "average_grade").write[scala.Option[Int]] and
        (__ \ "total_open_incidents").write[Long] and
        (__ \ "total_incidents").write[Long] and
        (__ \ "total_plans").write[Long] and
        (__ \ "plans").write[scala.collection.Seq[Plan]]
      )(unlift(Statistic.unapply _))
    }

    implicit def jsonReadsQualityTeam: play.api.libs.json.Reads[Team] = {
      (__ \ "key").read[String].map { x => new Team(key = x) }
    }

    implicit def jsonWritesQualityTeam: play.api.libs.json.Writes[Team] = new play.api.libs.json.Writes[Team] {
      def writes(x: Team) = play.api.libs.json.Json.obj(
        "key" -> play.api.libs.json.Json.toJson(x.key)
      )
    }
  }
}
