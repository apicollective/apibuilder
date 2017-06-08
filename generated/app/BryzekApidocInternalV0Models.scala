/**
 * Generated by apidoc - http://www.apidoc.me
 * Service version: 0.11.80
 * apidoc:0.11.88 http://www.apidoc.me/bryzek/apidoc-internal/0.11.80/play_2_x_json
 */
package com.bryzek.apidoc.internal.v0.models {

  sealed trait TaskData extends _root_.scala.Product with _root_.scala.Serializable

  case class Task(
    guid: _root_.java.util.UUID,
    data: com.bryzek.apidoc.internal.v0.models.TaskData,
    numberAttempts: Long = 0,
    lastError: _root_.scala.Option[String] = None
  )

  case class TaskDataDiffVersion(
    oldVersionGuid: _root_.java.util.UUID,
    newVersionGuid: _root_.java.util.UUID
  ) extends TaskData

  case class TaskDataIndexApplication(
    applicationGuid: _root_.java.util.UUID
  ) extends TaskData

  /**
   * Provides future compatibility in clients - in the future, when a type is added
   * to the union TaskData, it will need to be handled in the client code. This
   * implementation will deserialize these future types as an instance of this class.
   */
  case class TaskDataUndefinedType(
    description: String
  ) extends TaskData

}

package com.bryzek.apidoc.internal.v0.models {

  package object json {
    import play.api.libs.json.__
    import play.api.libs.json.JsString
    import play.api.libs.json.Writes
    import play.api.libs.functional.syntax._
    import com.bryzek.apidoc.internal.v0.models.json._

    private[v0] implicit val jsonReadsUUID = __.read[String].map(java.util.UUID.fromString)

    private[v0] implicit val jsonWritesUUID = new Writes[java.util.UUID] {
      def writes(x: java.util.UUID) = JsString(x.toString)
    }

    private[v0] implicit val jsonReadsJodaDateTime = __.read[String].map { str =>
      import org.joda.time.format.ISODateTimeFormat.dateTimeParser
      dateTimeParser.parseDateTime(str)
    }

    private[v0] implicit val jsonWritesJodaDateTime = new Writes[org.joda.time.DateTime] {
      def writes(x: org.joda.time.DateTime) = {
        import org.joda.time.format.ISODateTimeFormat.dateTime
        val str = dateTime.print(x)
        JsString(str)
      }
    }

    implicit def jsonReadsApidocinternalTask: play.api.libs.json.Reads[Task] = {
      (
        (__ \ "guid").read[_root_.java.util.UUID] and
        (__ \ "data").read[com.bryzek.apidoc.internal.v0.models.TaskData] and
        (__ \ "number_attempts").read[Long] and
        (__ \ "last_error").readNullable[String]
      )(Task.apply _)
    }

    def jsObjectTask(obj: com.bryzek.apidoc.internal.v0.models.Task): play.api.libs.json.JsObject = {
      play.api.libs.json.Json.obj(
        "guid" -> play.api.libs.json.JsString(obj.guid.toString),
        "data" -> jsObjectTaskData(obj.data),
        "number_attempts" -> play.api.libs.json.JsNumber(obj.numberAttempts)
      ) ++ (obj.lastError match {
        case None => play.api.libs.json.Json.obj()
        case Some(x) => play.api.libs.json.Json.obj("last_error" -> play.api.libs.json.JsString(x))
      })
    }

    implicit def jsonWritesApidocinternalTask: play.api.libs.json.Writes[Task] = {
      new play.api.libs.json.Writes[com.bryzek.apidoc.internal.v0.models.Task] {
        def writes(obj: com.bryzek.apidoc.internal.v0.models.Task) = {
          jsObjectTask(obj)
        }
      }
    }

    implicit def jsonReadsApidocinternalTaskDataDiffVersion: play.api.libs.json.Reads[TaskDataDiffVersion] = {
      (
        (__ \ "old_version_guid").read[_root_.java.util.UUID] and
        (__ \ "new_version_guid").read[_root_.java.util.UUID]
      )(TaskDataDiffVersion.apply _)
    }

    def jsObjectTaskDataDiffVersion(obj: com.bryzek.apidoc.internal.v0.models.TaskDataDiffVersion): play.api.libs.json.JsObject = {
      play.api.libs.json.Json.obj(
        "old_version_guid" -> play.api.libs.json.JsString(obj.oldVersionGuid.toString),
        "new_version_guid" -> play.api.libs.json.JsString(obj.newVersionGuid.toString)
      )
    }

    implicit def jsonReadsApidocinternalTaskDataIndexApplication: play.api.libs.json.Reads[TaskDataIndexApplication] = {
      (__ \ "application_guid").read[_root_.java.util.UUID].map { x => new TaskDataIndexApplication(applicationGuid = x) }
    }

    def jsObjectTaskDataIndexApplication(obj: com.bryzek.apidoc.internal.v0.models.TaskDataIndexApplication): play.api.libs.json.JsObject = {
      play.api.libs.json.Json.obj(
        "application_guid" -> play.api.libs.json.JsString(obj.applicationGuid.toString)
      )
    }

    implicit def jsonReadsApidocinternalTaskData: play.api.libs.json.Reads[TaskData] = {
      (
        (__ \ "task_data_index_application").read(jsonReadsApidocinternalTaskDataIndexApplication).asInstanceOf[play.api.libs.json.Reads[TaskData]]
        orElse
        (__ \ "task_data_diff_version").read(jsonReadsApidocinternalTaskDataDiffVersion).asInstanceOf[play.api.libs.json.Reads[TaskData]]
        orElse
        play.api.libs.json.Reads(jsValue => play.api.libs.json.JsSuccess(com.bryzek.apidoc.internal.v0.models.TaskDataUndefinedType(jsValue.toString))).asInstanceOf[play.api.libs.json.Reads[TaskData]]
      )
    }

    def jsObjectTaskData(obj: com.bryzek.apidoc.internal.v0.models.TaskData): play.api.libs.json.JsObject = {
      obj match {
        case x: com.bryzek.apidoc.internal.v0.models.TaskDataIndexApplication => play.api.libs.json.Json.obj("task_data_index_application" -> jsObjectTaskDataIndexApplication(x))
        case x: com.bryzek.apidoc.internal.v0.models.TaskDataDiffVersion => play.api.libs.json.Json.obj("task_data_diff_version" -> jsObjectTaskDataDiffVersion(x))
        case x: com.bryzek.apidoc.internal.v0.models.TaskDataUndefinedType => sys.error(s"The type[com.bryzek.apidoc.internal.v0.models.TaskDataUndefinedType] should never be serialized")
      }
    }

    implicit def jsonWritesApidocinternalTaskData: play.api.libs.json.Writes[TaskData] = {
      new play.api.libs.json.Writes[com.bryzek.apidoc.internal.v0.models.TaskData] {
        def writes(obj: com.bryzek.apidoc.internal.v0.models.TaskData) = {
          jsObjectTaskData(obj)
        }
      }
    }
  }
}

package com.bryzek.apidoc.internal.v0 {

  object Bindables {

    import play.api.mvc.{PathBindable, QueryStringBindable}
    import org.joda.time.{DateTime, LocalDate}
    import org.joda.time.format.ISODateTimeFormat
    import com.bryzek.apidoc.internal.v0.models._

    // Type: date-time-iso8601
    implicit val pathBindableTypeDateTimeIso8601 = new PathBindable.Parsing[org.joda.time.DateTime](
      ISODateTimeFormat.dateTimeParser.parseDateTime(_), _.toString, (key: String, e: _root_.java.lang.Exception) => s"Error parsing date time $key. Example: 2014-04-29T11:56:52Z"
    )

    implicit val queryStringBindableTypeDateTimeIso8601 = new QueryStringBindable.Parsing[org.joda.time.DateTime](
      ISODateTimeFormat.dateTimeParser.parseDateTime(_), _.toString, (key: String, e: _root_.java.lang.Exception) => s"Error parsing date time $key. Example: 2014-04-29T11:56:52Z"
    )

    // Type: date-iso8601
    implicit val pathBindableTypeDateIso8601 = new PathBindable.Parsing[org.joda.time.LocalDate](
      ISODateTimeFormat.yearMonthDay.parseLocalDate(_), _.toString, (key: String, e: _root_.java.lang.Exception) => s"Error parsing date $key. Example: 2014-04-29"
    )

    implicit val queryStringBindableTypeDateIso8601 = new QueryStringBindable.Parsing[org.joda.time.LocalDate](
      ISODateTimeFormat.yearMonthDay.parseLocalDate(_), _.toString, (key: String, e: _root_.java.lang.Exception) => s"Error parsing date $key. Example: 2014-04-29"
    )



  }

}
