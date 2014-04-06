package lib

import models.Operation

case class OperationKey(op: Operation) {

  def key: String = {
    op.path match {
      case None => UrlKey.generate(op.method)
      case Some(path) => UrlKey.generate(s"${op.method}-${path}")
    }
  }

}
