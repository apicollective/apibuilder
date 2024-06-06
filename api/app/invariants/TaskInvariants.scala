package invariants

import io.flow.postgresql.Query

object TaskInvariants {
  val all: Seq[Invariant] = Seq(
    Invariant(
      "tasks_not_attempted",
      Query("""
        |select count(*) from tasks where num_attempts=0 and created_at < now() - interval '1 hour'
        |""".stripMargin)
    ),
    Invariant(
      "tasks_not_completed_in_12_hours",
      Query("""
        |select count(*) from tasks where num_attempts>0 and created_at < now() - interval '12 hour'
        |""".stripMargin)
    )
  )
}
