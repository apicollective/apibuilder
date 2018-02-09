package controllers

import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}

class TokensSpec extends PlaySpec with MockClient with OneServerPerSuite {

  import scala.concurrent.ExecutionContext.Implicits.global

  "POST /tokens" in {
    val user = createUser()
    val form = createTokenForm(user).copy(description = Some("testing"))
    val token = await(newClient(user).tokens.post(form))
    token.user.guid must equal(user.guid)
    token.description must be(Some("testing"))
  }

  "GET /tokens" in {
    val user = createUser()
    val client = newClient(user)
    await(
      client.tokens.getUsersByUserGuid(userGuid = user.guid)
    ) must equal(Nil)

    val token = await(client.tokens.post(createTokenForm(user)))
    await(
      client.tokens.getUsersByUserGuid(userGuid = user.guid)
    ).map(_.guid) must equal(Seq(token.guid))
  }

  "GET /tokens restricts by user guid" in {
    val user1 = createUser()
    val tokenUser1 = await(newClient(user1).tokens.post(createTokenForm(user1)))

    val user2 = createUser()
    val tokenUser2 = await(newClient(user2).tokens.post(createTokenForm(user2)))

    await(
      newClient(user1).tokens.getUsersByUserGuid(userGuid = user1.guid)
    ).map(_.guid) must equal(Seq(tokenUser1.guid))
    await(
      newClient(user2).tokens.getUsersByUserGuid(userGuid = user2.guid)
    ).map(_.guid) must equal(Seq(tokenUser2.guid))
  }

  "GET /tokens/:guid/cleartext" in {
    val user = createUser()
    val token = await(newClient(user).tokens.post(createTokenForm(user)))

    val clear = await(
      newClient(user).tokens.getCleartextByGuid(token.guid)
    )
    clear.token.length > 30 must be(true)
  }
}
