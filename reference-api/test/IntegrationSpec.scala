import languageFeature.postfixOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.Future

import java.util.UUID

import org.specs2._
import org.specs2.mutable._
import org.specs2.runner._
import org.scalacheck._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

import org.joda.time.DateTime

import play.api.Logger

import referenceapi.Client
import referenceapi.models
import referenceapi.models._

import ch.qos.logback.classic.Level

/**
 * Integration spec for generated clients against the reference API.
 */
@RunWith(classOf[JUnitRunner])
class IntegrationSpec extends org.specs2.mutable.Specification with ScalaCheck {
  implicit def result[T](future: Future[T]) = Await.result(future, Duration.Inf)

  @inline // cutdown on boilerplate with optional query params
  implicit def x2option[X](x: X): Option[X] = Option(x)

  import referenceapi.test.models.Arbitrary._

  def withClient[T](f: Client => T): T = {
    val port = Helpers.testServerPort
    running(TestServer(port = port)) {
      // uncomment below for debugging
      // Logger.configure(
      //   levels = Map("referenceapi.client" -> Level.DEBUG),
      //   mode = play.api.Mode.Dev
      // )
      val client = new Client("http://localhost:" + port)
      f(client)
    }
  }

  "Application" should {

    "support the organization api" in prop { (organization: models.organization.OrganizationImpl) =>

      withClient { implicit client =>

        import client._

        result {
          Organizations.post(
            guid = organization.guid,
            name = organization.name
          )
        } match {
          case (201, o: Organization) => o must equalTo(organization)
        }

        result {
          Organizations.getByGuid(guid = organization.guid.toString)
        } match {
          case (200, o: Organization) => o must equalTo(organization)
        }

        result {
          Organizations.getByGuid(guid = UUID.randomUUID.toString)
        } match {
          case r: play.api.libs.ws.Response => r.status must equalTo(404)
        }

        result {
          Organizations.get(guid = organization.guid)
        } match {
          case (200, os: List[Organization]) => os.head must equalTo(organization)
        }

        result {
          Organizations.get(name = organization.name)
        } match {
          case (200, os: List[Organization]) => os.head must equalTo(organization)
        }

        result {
          Organizations.get(guid = UUID.randomUUID)
        } match {
          case (200, os: List[Organization]) => os must beEmpty
        }

        result {
          Organizations.get(name = "blah")
        } match {
          case (200, os: List[Organization]) => os must beEmpty
        }
      }
    }

    "should support the user api" in prop { (user: models.user.UserImpl) =>
      withClient { implicit client =>
        import client._

        result {
          Users.post(
            guid = user.guid,
            email = user.email,
            active = user.active
          )
        } match {
          case (201, u) => u must equalTo(user)
        }

        result {
          Users.get(
            guid = user.guid,
            active = user.active
          )
        } match {
          case (200, us: List[User]) => us must equalTo(List(user))
        }

        result {
          Users.get(
            guid = user.guid,
            active = !user.active
          )
        } match {
          case (200, us: List[User]) => us must equalTo(Nil)
        }

        result {
          Users.get(
            email = user.email,
            active = user.active
          )
        } match {
          case (200, us: List[User]) => us must equalTo(List(user))
        }

        result {
          Users.get(
            email = user.email,
            active = !user.active
          )
        } match {
          case (200, us: List[User]) => us must equalTo(Nil)
        }

        result {
          Users.get(
            active = user.active
          )
        } match {
          case (200, us: List[User]) => {
            us.foreach { u =>
              u.active must equalTo(user.active)
            }
            us must contain(user)
          }
        }

        result {
          Users.get(
            active = !user.active
          )
        } match {
          case (200, us: List[User]) => {
            us.foreach { u =>
              u.active must not equalTo(user.active)
            }
            us must not contain(user)
          }
        }
      }
    }

    "should support the member api" in prop { (
      organization: models.organization.OrganizationImpl,
      user: models.user.UserImpl,
      member: models.member.MemberImpl
    ) => pending
    }
  }
}
