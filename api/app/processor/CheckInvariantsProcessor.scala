package processor

import anorm.SqlParser
import cats.data.ValidatedNec
import cats.implicits._
import invariants.{Invariant, Invariants}
import io.apibuilder.task.v0.models._
import lib.{AppConfig, EmailUtil, Person}
import play.api.db.Database

import javax.inject.Inject

case class InvariantResult(invariant: Invariant, count: Long)

class CheckInvariantsProcessor @Inject()(
  args: TaskProcessorArgs,
  appConfig: AppConfig,
  invariants: Invariants,
  database: Database,
  email: EmailUtil,
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

  private[this] def sendResults(results: Seq[InvariantResult]): Unit = {
    val (noErrors, withErrors) = results.partition(_.count == 0)

    println(s"# Invariants checked with no errors: ${noErrors.length}")
    if (withErrors.nonEmpty) {
      lazy val subject = if (withErrors.length == 1) {
        "1 Error"
      } else {
        s"${withErrors.length} Errors"
      }
      lazy val body = views.html.emails.invariants(appConfig, noErrors.map(_.invariant.name), withErrors).toString
      appConfig.sendErrorsTo.foreach { recipientEmail =>
        email.sendHtml(
          to = Person(email = recipientEmail),
          subject = s"[API Builder Invariants] $subject",
          body = body
        )
      }
    }
  }
}