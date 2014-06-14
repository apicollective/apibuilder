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

        Organizations.post(organization).entity must equalTo(organization)

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

    "should default to creating active users" in prop { (userForm: UserForm) =>
      withClient { implicit client =>
        import client._
        Users.post(_body = userForm).entity.active must beTrue
      }
    }

    "should create active users" in prop { (userForm: UserForm) =>
      withClient { implicit client =>
        import client._
        Users.post(active = true, _body = userForm).entity.active must beTrue
      }
    }

    "should create inactive users" in prop { (userForm: UserForm) =>
      withClient { implicit client =>
        import client._
        Users.post(active = false, _body = userForm).entity.active must beFalse
      }
    }

    "should post profiles" in prop { (userForm: UserForm) =>
      withClient { implicit client =>
        import client._

        val user = Users.post(_body = userForm).entity
        import java.nio.file.Files
        import java.io.FileOutputStream
        val file = Files.createTempFile(user.guid.toString, ".profile").toFile
        val out = new FileOutputStream(file)
        out.write("Pices".getBytes)
        out.close
        Users.postProfileByGuid(user.guid, file).entity must equalTo(())
      }
    }

    "should patch by guid" in prop { (userForm: UserForm, patches: Seq[User.Patch]) =>
      withClient { implicit client =>
        import client._

        val user = Users.post(_body = userForm).entity
        val patched = Users.patchByGuid(user.guid, patches).entity
        patches.foldLeft(user) { case (user, patch) =>
          patch.copy(guid = None)(user)
        } must equalTo(patched)
        Users.get(
          guid = user.guid,
          active = patched.active).entity.head must equalTo(patched)
      }
    }

    "should support the user api" in prop { (userForm: UserForm) =>
      withClient { implicit client =>
        import client._

        val user = Users.post(
          active = true,
          _body = userForm).entity
        user.email must equalTo(userForm.email)
        user.active must beTrue

        Users.get(
          guid = user.guid,
          active = true
        ).entity must equalTo(List(user))

        Users.get(
          guid = user.guid,
          active = false
        ).entity must equalTo(Nil)

        Users.get(
          email = user.email,
          active = true
        ).entity must equalTo(List(user))

        Users.get(
          email = user.email,
          active = false
        ).entity must equalTo(Nil)

        {
          val us = Users.get(
            active = true
          ).entity
          us.foreach { u =>
            u.active must beTrue
          }
          us must contain(user)
        }

        {
          val us = Users.get(
            active = false
          ).entity
          us.foreach { u =>
            u.active must beFalse
          }
          us must not contain(user)
        }
      }
    }

    "should support the member api" in prop { (userForm: UserForm, organization: Organization, role: String) =>
      withClient { implicit client =>

        import client._

        Organizations.post(organization).entity
        val user = Users.post(_body = userForm).entity
        val memberForm = new MemberForm(
          organization = organization.guid,
          user = user.guid,
          role = role)
        val member = Members.post(memberForm).entity
        member.organization must equalTo(organization)
        member.user.email must equalTo(userForm.email)
        member.role must equalTo(role)

        Members.get(guid = member.guid).entity must equalTo(List(member))

        Members.get(organization = member.organization.guid)
          .entity must equalTo(List(member))

        Members.get(user = member.user.guid)
          .entity must equalTo(List(member))

        Members.get(role = member.role).entity must equalTo(List(member))

        Members.getByOrganization(member.organization.guid.toString)
          .entity must equalTo(List(member))
      }
    }
  }
}
