package db

import java.util.UUID

import org.joda.time._
import org.joda.time.format._

import anorm._

object Implicits {
  implicit def rowToUUID: Column[UUID] = Column.nonNull[UUID] { (value, meta) =>
    value match {
      case v: UUID => Right(v)
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value:${value.asInstanceOf[AnyRef].getClass} to UUID for column ${meta.column}"))
    }
  }

  implicit val uuidToStatement = new ToStatement[UUID] {
    def set(s: java.sql.PreparedStatement, index: Int, value: UUID): Unit = s.setObject(index, value)
  }

  val dateTimeFormatter = ISODateTimeFormat.dateTimeParser

  implicit def rowToDateTime: Column[DateTime] = Column.nonNull { (value, meta) =>
    value match {
        case ts: java.sql.Timestamp => Right(new DateTime(ts.getTime))
        case d: java.sql.Date => Right(new DateTime(d.getTime))
        case str: java.lang.String => Right(dateTimeFormatter.parseDateTime(str.trim))
        case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass) )
    }
  }

  implicit val dateTimeToStatement = new ToStatement[DateTime] {
    def set(s: java.sql.PreparedStatement, index: Int, aValue: DateTime): Unit = {
        s.setTimestamp(index, new java.sql.Timestamp(aValue.getMillis()) )
    }
  }

}
