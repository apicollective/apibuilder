package lib

object Util {

  def formatType(typeName: String, isMultiple: Boolean) = {
    if (isMultiple) {
      s"[$typeName]"
    } else {
      typeName
    }
  }

}
