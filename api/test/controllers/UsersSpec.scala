package controllers

import io.apibuilder.api.v0.models.UserUpdateForm
import java.util.UUID

import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.PlaySpec

class UsersSpec extends PlaySpec with MockClient with GuiceOneServerPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

  "POST /users" in {
    val form = makeUserForm()
    val user = await {
      client.users.post(form)
    }
    user.email must equal(form.email)
  }

  "POST /users/authenticate" in {
    val form = makeUserForm()
    createUser(form)

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
    updatedUser.name must be(Some("joseph"))
  }

  "GET /users by nickname" in {
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

}
