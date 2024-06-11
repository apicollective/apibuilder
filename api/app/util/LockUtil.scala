package util

import anorm._
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

  def lock[T](id: String)(f: Connection => T): Option[T] =
    db.withTransaction(lock(_)(id)(f))

  def lock[T](c: Connection)(id: String)(f: Connection => T): Option[T] = {
    require(!c.getAutoCommit, "Must be in a transaction")
    if (tryLockInternal(c)(id))
      Some(f(c))
    else
      None
  }

  private def tryLockInternal(c: Connection)(id: String): Boolean = {
    val (key1, key2) = LockUtil.toHashInts(id)
    Query("SELECT pg_try_advisory_xact_lock({key1}::int, {key2}::int)")
      .bind("key1", key1)
      .bind("key2", key2)
      .as(SqlParser.bool(1).single)(c)
  }
}

object LockUtil {
  def toHashInts(id: String): (Int, Int) = {
    val buffer = ByteBuffer.wrap(DigestUtils.md5(id))
    val key1 = buffer.getInt(0)
    val key2 = buffer.getInt(4)
    (key1, key2)
  }
}