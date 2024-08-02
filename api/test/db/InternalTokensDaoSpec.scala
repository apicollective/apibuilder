package db

import io.apibuilder.api.v0.models.TokenForm
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class InternalTokensDaoSpec extends PlaySpec with GuiceOneAppPerSuite with db.Helpers {

  "obfuscates token by default" in {
    val user = upsertUser()
    val token = expectValid {
      tokensDao.create(
        user,
        TokenForm(userGuid = user.guid)
      )
    }
    token.userGuid must be(user.guid)
    token.maskedToken must be("XXX-XXX-XXX")

    tokensDao.findByToken(token.db.token).get.guid must be(token.guid)
  }

}
