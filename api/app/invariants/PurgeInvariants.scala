package invariants

import io.flow.postgresql.Query
import org.joda.time.DateTime

object PurgeInvariants {
  private[this] case class PurgeTable(name: String, retentionMonths: Int)
  private[this] val Tables = Seq(
    PurgeTable("organizations", 12),
    PurgeTable("applications", 6),
    PurgeTable("versions", 6)
  )

  val all: Seq[Invariant] = Tables.map { t =>
    Invariant(
      s"old_deleted_records_purged_from_${t.name}",
      Query(
        s"select count(*) from ${t.name} where deleted_at < {deleted_at}::timestamptz"
      ).bind("deleted_at", DateTime.now.minusMonths(t.retentionMonths+1))
    )
  }
}
