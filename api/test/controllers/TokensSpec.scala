package controllers

import play.api.test._

class TokensSpec extends PlaySpecification with MockClient {
  import scala.concurrent.ExecutionContext.Implicits.global

  "POST /tokens" in new WithServer(port=defaultPort) {
    val user = createUser()
    val form = createTokenForm(user).copy(description = Some("testing"))
    val token = await(newClient(user).tokens.post(form))
    token.user.guid must beEqualTo(user.guid)
    token.description must beSome("testing")
  }

  "GET /tokens" in new WithServer(port=defaultPort) {
    val user = createUser()
    val client = newClient(user)
    await(
      client.tokens.getUsersByUserGuid(userGuid = user.guid)
    ) must beEqualTo(Nil)

    val token = await(client.tokens.post(createTokenForm(user)))
    await(
      client.tokens.getUsersByUserGuid(userGuid = user.guid)
    ).map(_.guid) must beEqualTo(Seq(token.guid))
  }

  "GET /tokens restricts by user guid" in new WithServer(port=defaultPort) {
    val user1 = createUser()
    val tokenUser1 = await(newClient(user1).tokens.post(createTokenForm(user1)))

    val user2 = createUser()
    val tokenUser2 = await(newClient(user2).tokens.post(createTokenForm(user2)))

    await(
      newClient(user1).tokens.getUsersByUserGuid(userGuid = user1.guid)
    ).map(_.guid) must beEqualTo(Seq(tokenUser1.guid))
    await(
      newClient(user2).tokens.getUsersByUserGuid(userGuid = user2.guid)
    ).map(_.guid) must beEqualTo(Seq(tokenUser2.guid))
  }

  "GET /tokens/:guid/cleartext" in new WithServer(port=defaultPort) {
    val user = createUser()
    val token = await(newClient(user).tokens.post(createTokenForm(user)))

    val clear = await(
      newClient(user).tokens.getCleartextByGuid(token.guid)
    )
    clear.token.length > 30 must beTrue
  }
}
