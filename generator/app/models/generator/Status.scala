package generator

sealed trait Status

object Status {
  case object Alpha extends Status
  case object Beta extends Status
  case object InDevelopment extends Status
  case object Proposal extends Status
}
