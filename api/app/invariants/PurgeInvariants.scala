package invariants

import io.flow.postgresql.Query

object PurgeInvariants {
  private[this] val Tables = Seq(
    "organizations", "applications", "versions"
  )

  val all: Seq[Invariant] = Tables.map { t =>
    Invariant(
      s"old_deleted_records_purged_from_$t",
      Query(
        s"select count(*) from $t where deleted_at < now() - interval '3 months'"
      )
    )
  }
}
