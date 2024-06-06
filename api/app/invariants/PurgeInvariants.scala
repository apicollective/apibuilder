package invariants

import io.flow.postgresql.Query

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
        s"select count(*) from ${t.name} where deleted_at < now() - interval '${t.retentionMonths + 1} months'"
      )
    )
  }
}
