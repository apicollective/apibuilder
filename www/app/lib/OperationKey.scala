package lib

import core.{ Operation, ServiceDescription, UrlKey }

object OperationKey {

  def lookup(service: ServiceDescription, key: String): Option[Operation] = {
    service.operations.find { op =>
      OperationKey(op).key == key
    }
  }

}

case class OperationKey(op: Operation) {

  lazy val key = UrlKey.generate(op.label)

}
