package invariants

import anorm.SqlParser
import play.api.db.Database

import javax.inject.Inject

/** Sync all invoices with a balance
  */
class CheckInvariants @Inject() (
  invariants: Invariants,
  database: Database,
) {

  def process(): Unit = {
    val results = invariants.all.map { i =>
      val count = database.withConnection { c =>
        i.query.as(SqlParser.long(1).*)(c).headOption.getOrElse(0L)
      }
      InvariantResult(i, count)
    }
    sendResults(results)
  }

  private[this] case class InvariantResult(invariant: Invariant, count: Long)
  private[this] def sendResults(results: Seq[InvariantResult]): Unit = {
    val (_, withErrors) = results.partition(_.count == 0)

    if (withErrors.nonEmpty) {
      val subject = if (withErrors.length == 1) {
        "1 Error"
      } else {
        s"${withErrors.length} Errors"
      }
      println(subject)
      withErrors.foreach { e =>
        println(s"${e.invariant.name}: ${e.count}")
        println(s"${e.invariant.query.interpolate()}")
        println("")
      }
    }
  }
}
