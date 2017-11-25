package controllers

import io.apibuilder.api.v0.models.UserUpdateForm
import java.util.UUID
import play.api.test._

class UsersSpec extends BaseSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  "POST /users" in new WithServer {
    val form = createUserForm()
    println("SPEC STARTING AWAIT")
    val user = await {
      client.users.post(form)
    }
    println("SPEC FINISHED AWAIT")
    user.email must equal(form.email)
  }
/*
  "POST /users/authenticate" in new WithServer {
    val form = createUserForm()
    val user = createUser(form)

    // Need to wait longer here as these methods use bcrypt
    val auth = await(
      client.users.postAuthenticate(form.email, form.password)
    )

    val updatedUser = await {
      newSessionClient(auth.session.id).users.putByGuid(
        auth.user.guid,
        UserUpdateForm(
          email = auth.user.email,
          nickname = auth.user.nickname,
          name = Some("joseph")
        )
      )
    }
    updatedUser.name must equal(Some("joseph"))
  }

  "GET /users by nickname" in new WithServer {
    val user1 = createUser()
    val user2 = createUser()

    await(
      client.users.get(nickname = Some(user1.nickname))
    ).map(_.guid) must equal(Seq(user1.guid))

    await(
      client.users.get(nickname = Some(user2.nickname))
    ).map(_.guid) must equal(Seq(user2.guid))

    await(
      client.users.get(nickname = Some(UUID.randomUUID.toString))
    ).map(_.guid) must be(Nil)
  }
*/
}
