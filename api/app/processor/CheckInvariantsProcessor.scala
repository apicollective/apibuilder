package processor

import anorm.SqlParser
import cats.data.ValidatedNec
import cats.implicits._
import invariants.{Invariant, Invariants}
import io.apibuilder.task.v0.models._
import play.api.db.Database

import javax.inject.Inject


class CheckInvariantsProcessor @Inject()(
  args: TaskProcessorArgs,
  invariants: Invariants,
  database: Database,
) extends TaskProcessor(args, TaskType.CheckInvariants) {

  override def processRecord(id: String): ValidatedNec[String, Unit] = {
    val results = invariants.all.map { i =>
      val count = database.withConnection { c =>
        i.query.as(SqlParser.long(1).*)(c).headOption.getOrElse(0L)
      }
      InvariantResult(i, count)
    }
    sendResults(results)
    ().validNec
  }

  private[this] case class InvariantResult(invariant: Invariant, count: Long)
  private[this] def sendResults(results: Seq[InvariantResult]): Unit = {
    val (noErrors, withErrors) = results.partition(_.count == 0)

    println(s"# Invariants checked with no errors: ${noErrors.length}")
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