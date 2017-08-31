package controllers

import io.apibuilder.api.v0.models.UserUpdateForm
import java.util.UUID
import play.api.test._

class UsersSpec extends PlaySpecification with MockClient {

  import scala.concurrent.ExecutionContext.Implicits.global

  "POST /users/authenticate" in new WithServer(port = defaultPort) {
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

  "GET /users by bickname" in new WithServer(port = defaultPort) {
    val user1 = createUser()
    val user2 = createUser()

    await(
      client.users.get(nickname = Some(user1.nickname))
    ).map(_.guid) must beEqualTo(Seq(user1.guid))

    await(
      client.users.get(nickname = Some(user2.nickname))
    ).map(_.guid) must beEqualTo(Seq(user2.guid))

    await(
      client.users.get(nickname = Some(UUID.randomUUID.toString))
    ).map(_.guid) must be(Nil)
  }
}
