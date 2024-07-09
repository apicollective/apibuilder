package lib

import com.github.tototoshi.csv.{CSVFormat, CSVWriter}
import com.github.tototoshi.csv.defaultCSVFormat

import java.io.File
import io.apibuilder.api.v0.models.Membership

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.*

case class MemberDownload(
  client: io.apibuilder.api.v0.Client,
  orgKey: String
) {

  def csv()(implicit ec: ExecutionContext): Future[File] = Future {
    val file = File.createTempFile(s"member-download-$orgKey", "csv")

    val writer = CSVWriter.open(file)(defaultCSVFormat)
    writer.writeRow(Seq("guid", "role", "user_guid", "user_email", "user_nickname", "user_name"))

    Pager.eachPage[Membership] { offset =>
      Await.result(
        client.memberships.get(
          orgKey = Some(orgKey),
          limit = 250,
          offset = offset
        ),
        5000.millis
      )
    } { membership =>
      writer.writeRow(
        Seq(
          membership.guid,
          membership.role,
          membership.user.guid,
          membership.user.email,
          membership.user.nickname,
          membership.user.name.getOrElse("")
        )
      )
    }

    writer.close()

    file
  }

}
