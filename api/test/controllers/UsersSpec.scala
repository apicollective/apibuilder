package controllers

import io.apibuilder.api.v0.models.UserUpdateForm
import play.api.test._
import play.api.test.Helpers._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class UsersSpec extends BaseSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  "POST /users" in new WithServer {
    val form = createUserForm()
    val user = Await.result(
      client.users.post(form),
      Duration.Inf
    )
    user.email must equal(form.email)
  }

  "POST /users/authenticate" in new WithServer {
    val form = createUserForm()
    val user = createUser(form)

    // Need to wait longer here as these methods use bcrypt
    val auth = Await.result(
      client.users.postAuthenticate(form.email, form.password),
      Duration.Inf
    )
    println("RECEIVED AUTH: " + auth)

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
