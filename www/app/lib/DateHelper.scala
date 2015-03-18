package lib

import models.UserTimeZone
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.DateTimeFormat
import com.gilt.apidoc.api.v0.models.User

object DateHelper {

  private val DefaultLabel = "N/A"

  def shortDate(
    tz: UserTimeZone,
    dateTime: DateTime
  ): String = shortDateOption(tz, Some(dateTime))

  def shortDateOption(
    tz: UserTimeZone,
    dateTime: Option[DateTime],
    default: String = DefaultLabel
  ): String = {
    dateTime match {
      case None => default
      case Some(dt) => {
        DateTimeFormat.shortDate.withZone(DateTimeZone.forID(tz.name)).print(dt)
      }
    }
  }

  def mediumDateTime(
    tz: UserTimeZone,
    dateTime: DateTime
  ): String = mediumDateTimeOption(tz, Some(dateTime))

  def mediumDateTimeOption(
    tz: UserTimeZone,
    dateTime: Option[DateTime],
    default: String = DefaultLabel
  ): String = {
    dateTime match {
      case None => default
      case Some(dt) => {
        DateTimeFormat.mediumDateTime.withZone(DateTimeZone.forID(tz.name)).print(dt) + s" ${tz.label}"
      }
    }
  }

  def longDateTime(
    tz: UserTimeZone,
    dateTime: DateTime
  ): String = longDateTimeOption(tz, Some(dateTime))

  def longDateTimeOption(
    tz: UserTimeZone,
    dateTime: Option[DateTime],
    default: String = DefaultLabel
  ): String = {
    dateTime match {
      case None => default
      case Some(dt) => {
        DateTimeFormat.longDateTime.withZone(DateTimeZone.forID(tz.name)).print(dt)
      }
    }
  }

}
