package lib

import io.apibuilder.api.v0.models.User

import java.util.UUID
import java.nio.file.{Path, Paths, Files}
import java.nio.charset.StandardCharsets
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import com.sendgrid._


case class Person(email: String, name: Option[String] = None)

object Person {
  def apply(user: User): Person = Person(
    email = user.email,
    name = user.name
  )
}

object Email {

  private[this] val subjectPrefix = Config.requiredString("mail.subjectPrefix")

  private[this] val fromPerson = Person(
    email = Config.requiredString("mail.defaultFromEmail"),
    name = Some(Config.requiredString("mail.defaultFromName"))
  )

  val localDeliveryDir = Config.optionalString("mail.localDeliveryDir").map(Paths.get(_))

  // Initialize sendgrid on startup to verify that all of our settings
  // are here. If using localDeliveryDir, set password to a test
  // string.
  private[this] val sendgrid = {
    localDeliveryDir match {
      case None => new SendGrid(Config.requiredString("sendgrid.apiKey"))
      case Some(_) => new SendGrid(Config.optionalString("sendgrid.apiKey").getOrElse("development"))
    }
  }

  def sendHtml(
    to: Person,
    subject: String,
    body: String
  ) {
    val prefixedSubject = subjectPrefix + " " + subject

    val from = fromPerson.name match {
      case Some(n) => new com.sendgrid.Email(fromPerson.email, n)
      case None => new com.sendgrid.Email(fromPerson.email)
    }

    val recipient = to.name match {
      case Some(n) => new com.sendgrid.Email(to.email, n)
      case None => new com.sendgrid.Email(to.email)
    }

    val content = new Content("text/html", body)

    val mail = new Mail(from, prefixedSubject, recipient, content)

    localDeliveryDir match {
      case Some(dir) => {
        localDelivery(dir, to, prefixedSubject, body)
      }

      case None => {
        val request = new Request()
        request.setMethod(Method.POST)
        request.setEndpoint("mail/send")
        request.setBody(mail.build())
        val response = sendgrid.api(request)
        assert(
          response.getStatusCode() == 202,
          "Error sending email. Expected statusCode[202] but got[${response.getStatusCode()}]"
        )
      }
    }
  }

  private[this] def localDelivery(dir: Path, to: Person, subject: String, body: String): String = {
    val timestamp = UrlKey.generate(ISODateTimeFormat.dateTimeNoMillis.print(DateTime.now))

    Files.createDirectories(dir)
    val target = Paths.get(dir.toString, timestamp + "-" + UUID.randomUUID.toString + ".html")
    val name = to.name match {
      case None => to.email
      case Some(name) => s""""$name" <${to.email}">"""
    }

    val bytes = s"""<p>
To: $name<br/>
Subject: $subject
</p>
<hr size="1"/>

$body
""".getBytes(StandardCharsets.UTF_8)
    Files.write(target, bytes)

    println(s"email delivered locally to $target")
    s"local-delivery-to-$target"
  }

}
