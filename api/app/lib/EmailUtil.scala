package lib

import io.apibuilder.api.v0.models.User

import java.util.UUID
import java.nio.file.{Files, Path, Paths}
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import com.sendgrid.*
import com.sendgrid.helpers.mail.*
import com.sendgrid.helpers.mail.objects.*
import db.InternalUser


case class Person(email: String, name: Option[String] = None)

object Person {
  def apply(user: InternalUser): Person = Person(
    email = user.email,
    name = user.name
  )

  def apply(user: User): Person = Person(
    email = user.email,
    name = user.name
  )
}

class EmailUtil @Inject()(
  appConfig: AppConfig
) {

  private val fromPerson = Person(
    email = appConfig.emailDefaultFromEmail,
    name = Some(appConfig.emailDefaultFromName)
  )

  private val localDeliveryDir: Option[Path] = appConfig.mailLocalDeliveryDir.map(Paths.get(_))

  // Initialize sendgrid on startup to verify that all of our settings
  // are here. If using localDeliveryDir, set password to a test
  // string.
  private val sendgrid = {
    localDeliveryDir match {
      case None => new SendGrid(appConfig.sendgridConfig.getOrElse {
        sys.error(s"sendgridConfig required when localDeliveryDir is not set")
      }.apiKey)
      case Some(_) => new SendGrid(appConfig.sendgridConfig.map(_.apiKey).getOrElse("development"))
    }
  }

  def sendHtml(
    to: Person,
    subject: String,
    body: String
  ): Unit = {
    val prefixedSubject = appConfig.subjectPrefix + " " + subject

    val from = fromPerson.name match {
      case Some(n) => new Email(fromPerson.email, n)
      case None => new Email(fromPerson.email)
    }

    val recipient = to.name match {
      case Some(n) => new Email(to.email, n)
      case None => new Email(to.email)
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
          response.getStatusCode == 202,
          "Error sending email. Expected statusCode[202] but got[${response.getStatusCode()}]"
        )
      }
    }
  }

  private def localDelivery(dir: Path, to: Person, subject: String, body: String): String = {
    val timestamp = UrlKey.generate(ISODateTimeFormat.dateTimeNoMillis.print(DateTime.now))

    Files.createDirectories(dir)
    val target = Paths.get(dir.toString, timestamp + "-" + UUID.randomUUID.toString + ".html")
    val name = to.name match {
      case None => to.email
      case Some(n) => s""""$n" <${to.email}">"""
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
