package db

object Filters {

  def isDeleted(
    tableName: String,
    value: Boolean
  ): String = {
    if (value) {
      s"$tableName.deleted_at is not null"
    } else {
      s"$tableName.deleted_at is null"
    }
  }

  def isExpired(
    tableName: String,
    value: Boolean
  ): String = {
    if (value) {
      s"$tableName.expires_at < now()"
    } else {
      s"$tableName.expires_at >= now()"
    }
  }

}
