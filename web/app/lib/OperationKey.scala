package lib

import core.{ Operation, Resource }

object OperationKey {

  def lookup(resource: Resource, key: String): Option[Operation] = {
    resource.operations.find { op =>
      OperationKey(op).key == key
    }
  }

}

case class OperationKey(op: Operation) {

  def key: String = {
    op.path match {
      case None => UrlKey.generate(op.method)
      case Some(path) => UrlKey.generate(s"${op.method}-${path}")
    }
  }

}
