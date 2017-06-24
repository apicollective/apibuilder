package lib

import io.apibuilder.apidoc.api.v0.Client
import io.apibuilder.apidoc.api.v0.models.{Membership, User}
import com.github.tototoshi.csv.CSVWriter
import java.io.File
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

case class MemberDownload(
  sessionId: String,
  orgKey: String
) {

  import scala.concurrent.ExecutionContext.Implicits.global

  def csv(): Future[File] = {
    val file = File.createTempFile(s"member-download-$orgKey", "csv")

    val writer = CSVWriter.open(file)
    writer.writeRow(Seq("guid", "role", "user_guid", "user_email", "user_nickname", "user_name"))

    val client = ApiClient(Some(sessionId)).client

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

    // TODO: Truly make async
    Future { file }
  }

}
