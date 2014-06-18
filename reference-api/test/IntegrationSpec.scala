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
  // This implicit let's you unwrap the Future as is done everywhere in this file
  // I will admit, that I discoverred this by accident, but it's still awesome.
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

    "support the organization api" in prop { (organization: models.Organization) =>

      withClient { implicit client =>

        import client._

        Organizations.post(
          guid = organization.guid,
          name = organization.name
        ).entity must equalTo(organization)

        Organizations.getByGuid(guid = organization.guid.toString)
          .entity must equalTo(organization)

        Organizations.getByGuid(guid = UUID.randomUUID.toString).recover[Any] {
          case r: FailedResponse[_] => r.status must equalTo(404)
        }

        Organizations.get(guid = organization.guid)
          .entity.head must equalTo(organization)

        Organizations.get(name = organization.name)
          .entity.head must equalTo(organization)

        Organizations.get(guid = UUID.randomUUID)
          .entity must beEmpty

        Organizations.get(name = "blah")
          .entity must beEmpty
      }
    }

    "should support the user api" in prop { (user: models.User) =>
      withClient { implicit client =>
        import client._

        Users.post(
          guid = user.guid,
          email = user.email,
          active = user.active
        ).entity must equalTo(user)

        Users.get(
          guid = user.guid,
          active = user.active
        ).entity must equalTo(List(user))

        Users.get(
          guid = user.guid,
          active = !user.active
        ).entity must equalTo(Nil)

        Users.get(
          email = user.email,
          active = user.active
        ).entity must equalTo(List(user))

        Users.get(
          email = user.email,
          active = !user.active
        ).entity must equalTo(Nil)

        {
          val us = Users.get(
            active = user.active
          ).entity
          us.foreach { u =>
            u.active must equalTo(user.active)
          }
          us must contain(user)
        }

        {
          val us = Users.get(
            active = !user.active
          ).entity
          us.foreach { u =>
            u.active must not equalTo(user.active)
          }
          us must not contain(user)
        }
      }
    }

    "should support the member api" in prop { (member: models.Member) =>
      withClient { implicit client =>

        import client._

        Organizations.post(
          guid = member.organization.guid,
          name = member.organization.name
        ).entity must equalTo(member.organization)

        Users.post(
          guid = member.user.guid,
          email = member.user.email,
          active = member.user.active
        ).entity must equalTo(member.user)

        Members.post(
          guid = member.guid,
          organization = member.organization.guid,
          user = member.user.guid,
          role = member.role
        ).entity must equalTo(member)

        Members.get(guid = member.guid).entity must equalTo(List(member))

        Members.get(organizationGuid = member.organization.guid)
          .entity must equalTo(List(member))

        Members.get(userGuid = member.user.guid)
          .entity must equalTo(List(member))

        Members.get(role = member.role).entity must equalTo(List(member))

        Members.getByOrganization(member.organization.guid.toString)
          .entity must equalTo(List(member))
      }
    }
  }
}
