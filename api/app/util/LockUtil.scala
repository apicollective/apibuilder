package util

import anorm._
import cats.data.ValidatedNec
import cats.implicits._
import io.flow.postgresql.Query
import org.apache.commons.codec.digest.DigestUtils
import play.api.db.Database

import java.nio.ByteBuffer
import java.sql.Connection
import javax.inject.{Inject, Singleton}

@Singleton
class LockUtil @Inject() (
  db: Database
) {

  def lock[T](id: String)(f: Connection => T): ValidatedNec[String, T] =
    db.withTransaction(lock(_)(id)(f))

  def lock[T](c: Connection)(id: String)(f: Connection => T): ValidatedNec[String, T] = {
    require(!c.getAutoCommit, "Must be in a transaction")
    if (acquireLock(c)(id))
      f(c).validNec
    else
      s"Failed to acquire lock for id '$id'".invalidNec
  }

  private def acquireLock(c: Connection)(id: String): Boolean = {
    val (key1, key2) = toHashInts(id)
    Query("SELECT pg_try_advisory_xact_lock({key1}::int, {key2}::int)")
      .bind("key1", key1)
      .bind("key2", key2)
      .as(SqlParser.bool(1).single)(c)
  }

  private[this] def toHashInts(id: String): (Int, Int) = {
    val buffer = ByteBuffer.wrap(DigestUtils.md5(id))
    val key1 = buffer.getInt(0)
    val key2 = buffer.getInt(4)
    (key1, key2)
  }
}