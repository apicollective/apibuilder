package invariants

import io.flow.postgresql.Query
import play.api.Environment

import javax.inject.Inject

case class Invariant(name: String, query: Query)

class Invariants @Inject() (
                           env: Environment
                           ) {
  val all: Seq[Invariant] =
    TaskInvariants.all ++ PurgeInvariants.all(env)
}
