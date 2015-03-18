package lib

import com.gilt.apidoc.api.v0.models.{Original, OriginalForm, OriginalType}

object OriginalUtil {

  def toOriginal(form: OriginalForm): Original = {
    Original(
      `type` = form.`type`.getOrElse(
        guessType(form.data).getOrElse(OriginalType.ApiJson)
      ),
      data = form.data
    )
  }

  /**
    * Attempts to guess the type of original based on the data
    */
  def guessType(data: String): Option[OriginalType] = {
    val trimmed = data.trim
    if (trimmed.indexOf("protocol ") >= 0 || trimmed.indexOf("@namespace") >= 0) {
      Some(OriginalType.AvroIdl)
    } else if (trimmed.startsWith("{")) {
      Some(OriginalType.ApiJson)
    } else {
      None
    }
  }

}
