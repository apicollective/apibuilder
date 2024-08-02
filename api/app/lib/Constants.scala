package lib

import java.util.UUID

object Constants {

  val DefaultUserGuid: UUID = UUID.fromString("f3973f60-be9f-11e3-b1b6-0800200c9a66")

  val AdminUserEmails: Seq[String] = Seq("admin@apibuilder.io")

  val AdminUserGuid: UUID = DefaultUserGuid

}
