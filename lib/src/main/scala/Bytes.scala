package lib

object Bytes {

  private[this] val OneKb = 1024
  private[this] val OneMb = OneKb * OneKb
  private[this] val OneGb = OneMb * OneKb

  def label(bytes: Long): String = {
    if (bytes < OneKb) {
      bytes match {
        case 1 => "1 byte"
        case n => s"$n bytes"
      }
    } else if (bytes < OneMb) {
      formatLabel(bytes, OneKb, "kb", "mb")
    } else if (bytes < OneGb) {
      formatLabel(bytes, OneMb, "mb", "gb")
    } else {
      formatLabel(bytes, OneGb, "gb", "tb")
    }
  }

  private[this] def formatLabel(bytes: Long, divisor: Long, label: String, nextLabel: String): String = {
    val value = bytes / divisor
    val remainder = ((bytes % divisor) / (divisor * 1.0))
    (remainder * 10).round match {
      case 0 => s"$value $label"
      case 10 => s"1 $nextLabel"
      case remainder => s"$value.$remainder $label"
    }
  }

}
