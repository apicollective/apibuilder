package db

import io.apibuilder.api.v0.models.TokenForm
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}

class TokensDaoSpec extends PlaySpec with OneAppPerSuite with util.Daos {

  it("obfuscates token by default") {
    val user = Util.upsertUser()
    val token = tokensDao.create(
      user,
      TokenForm(userGuid = user.guid)
    )
    token.user.guid must be(user.guid)
    token.maskedToken must be("XXX-XXX-XXX")

    val clear = tokensDao.findCleartextByGuid(Authorization.All, token.guid).get
    tokensDao.findByToken(clear.token).get.guid must be(token.guid)
  }

}
