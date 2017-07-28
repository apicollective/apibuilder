package db

import java.util.UUID

import io.apibuilder.api.v0.models.TokenForm
import org.scalatest.{FunSpec, Matchers}

class TokensDaoSpec extends FunSpec with Matchers with util.Daos {

  it("obfuscate") {
    val token = tokensDao.create(
      usersDao.AdminUser,
      TokenForm(userGuid = UUID.randomUUID)
    )
    token.maskedToken should be("XXX-XXXX-XXXX")

    val clear = tokensDao.findCleartextByGuid(Authorization.All, token.guid).get
    tokensDao.findByToken(clear.token).get.guid should be(token.guid)
  }

}
