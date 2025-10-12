package invariants

import io.flow.postgresql.Query
import org.joda.time.DateTime

object GeneratorInvariants {
  val all: Seq[Invariant] = Seq(
    Invariant(
      s"deleted_services_have_deleted_generators",
      Query(
        """
          |select count(*)
          |  from generators.generators g
          |  join generators.services s on s.guid = g.service_guid
          | where s.deleted_at is not null
          |   and g.deleted_at is null
          |""".stripMargin
      ).withDebugging
    )
  )
}
