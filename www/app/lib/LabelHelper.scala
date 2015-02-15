package lib

import com.gilt.apidoc.v0.models.User

object LabelHelper {

  def user(user: User): String = {
    user.name.getOrElse(user.email)
  }

}
