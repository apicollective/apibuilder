package controllers

import akka.util.Timeout
import io.apibuilder.api.v0.models.UserUpdateForm
import play.api.test._
import play.api.test.Helpers._

import scala.concurrent.duration._

class UsersSpec extends BaseSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  "POST /users/authenticate" in new WithServer {
    val form = createUserForm()
    val user = createUser(form)

    val auth = await(
      client.users.postAuthenticate(form.email, form.password)
    )(Timeout(FiniteDuration(3, SECONDS)))

    println(auth)

    val updatedUser = await(
      newSessionClient(auth.session.id).users.putByGuid(
        auth.user.guid,
        UserUpdateForm(
          email = auth.user.email,
          nickname = auth.user.nickname,
          name = Some("joseph")
        )
      )
    )
    updatedUser.name must equal(Some("joseph"))
  }

}
