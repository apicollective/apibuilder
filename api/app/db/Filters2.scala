package db

object Filters2 {

  def isDeleted(
    tableName: String,
    value: Boolean
  ): String = {
    value match {
      case true => s"$tableName.deleted_at is not null"
      case false => s"$tableName.deleted_at is null"
    }
  }

  def isExpired(
    tableName: String,
    value: Boolean
  ): String = {
    value match {
      case true => { s"$tableName.expires_at < now()" }
      case false => { s"$tableName.expires_at >= now()" }
    }
  }

}
