package invariants

import io.flow.postgresql.Query
import org.joda.time.DateTime
import play.api.Environment
import processor.PurgeOldDeleted

object PurgeInvariants {
  private[this] case class PurgeTable(name: String, retentionMonths: Int)
  private[this] val Tables = Seq(
    PurgeTable("organizations", 12),
    PurgeTable("applications", 6),
    PurgeTable("versions", 6)
  )

  def all(env: Environment): Seq[Invariant] = Tables.map { t =>
    val timestamp = Seq(DateTime.now.minusMonths(t.retentionMonths+1), PurgeOldDeleted.cutoff(env)).max
    Invariant(
      s"old_deleted_records_purged_from_${t.name}",
      Query(
        s"select count(*) from ${t.name} where deleted_at < {cutoff}::timestamptz"
      ).bind("cutoff", timestamp)
    )
  }
}
