package controllers

import io.apibuilder.api.v0.models.UserUpdateForm
import play.api.test._

class UsersSpec extends PlaySpecification with MockClient {

  import scala.concurrent.ExecutionContext.Implicits.global

  "POST /users/authenticate" in new WithServer(port = port) {
    val form = createUserForm()
    val user = createUser(form)

    val auth = await(
      client.users.postAuthenticate(form.email, form.password)
    )

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
    updatedUser.name must beSome("joseph")
  }

}
