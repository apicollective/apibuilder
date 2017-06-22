package controllers

import com.bryzek.apidoc.api.v0.errors.{ErrorsResponse, FailedRequest}
import com.bryzek.apidoc.api.v0.models.UserUpdateForm

import play.api.test._
import play.api.test.Helpers._

class UsersSpec extends BaseSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  /*
TODO: Test failing we think with an injection error
  "POST /users/authenticate" in new WithServer {
    val form = createUserForm()
    val user = createUser(form)

    val auth = await {
      client.users.postAuthenticate(form.email, form.password)
    }
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
   */
}
