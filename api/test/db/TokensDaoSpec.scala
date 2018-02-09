package db

import io.apibuilder.api.v0.models.TokenForm
import org.scalatest.{FunSpec, Matchers}

class TokensDaoSpec extends FunSpec with Matchers with util.TestApplication {

  it("obfuscates token by default") {
    val user = Util.upsertUser()
    val token = tokensDao.create(
      user,
      TokenForm(userGuid = user.guid)
    )
    token.user.guid should be(user.guid)
    token.maskedToken should be("XXX-XXX-XXX")

    val clear = tokensDao.findCleartextByGuid(Authorization.All, token.guid).get
    tokensDao.findByToken(clear.token).get.guid should be(token.guid)
  }

}
