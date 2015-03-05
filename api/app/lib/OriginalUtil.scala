package lib

import com.gilt.apidoc.v0.models.{Original, OriginalForm, OriginalType}

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
    if (data.indexOf("protocol") == -1) {
      Some(OriginalType.AvroIdl)
    } else {
      Some(OriginalType.ApiJson)
    }
  }

}
