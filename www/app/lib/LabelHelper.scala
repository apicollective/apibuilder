package lib

import com.gilt.apidoc.v0.models.User

object LabelHelper {

  def user(user: User): String = {
    user.name.getOrElse(user.email)
  }

  def token(value: String): String = {
    if (value.size >= 15) {
      // 1st 3, mask, + last 4
      val letters = value.split("")
      letters.slice(0,4).mkString("") + "-XXXX-" + letters.slice(letters.size-4, letters.size).mkString("")
    } else {
      "XXXX-XXXX-XXXX"
    }
  }

}
