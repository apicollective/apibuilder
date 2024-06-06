package invariants

import io.flow.postgresql.Query

import javax.inject.Inject

case class Invariant(name: String, query: Query)

class Invariants @Inject() {
  val all: Seq[Invariant] =
    TaskInvariants.all ++ PurgeInvariants.all
}
