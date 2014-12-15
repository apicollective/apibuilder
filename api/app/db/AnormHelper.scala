package db

import anorm._
import org.joda.time._
import org.joda.time.format._

private[db] object AnormHelper {

  val dateTimeFormatter = ISODateTimeFormat.dateTimeParser

  implicit def rowToDateTime: Column[DateTime] = Column.nonNull { (value, meta) =>
    value match {
      case ts: java.sql.Timestamp => Right(new DateTime(ts.getTime))
      case d: java.sql.Date => Right(new DateTime(d.getTime))
      case str: java.lang.String => Right(dateTimeFormatter.parseDateTime(str.trim))
      case _ => Left(TypeDoesNotMatch(s"Cannot convert $value:${value.asInstanceOf[AnyRef].getClass} to DateTime for column ${meta.column}"))
    }
  }

  implicit val dateTimeToStatement = new ToStatement[DateTime] {
    def set(s: java.sql.PreparedStatement, index: Int, aValue: DateTime): Unit = {
      s.setTimestamp(index, new java.sql.Timestamp(aValue.getMillis()) )
    }
  }

}
