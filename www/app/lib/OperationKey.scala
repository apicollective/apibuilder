package lib

import com.gilt.apidocgenerator.models.{Operation, ServiceDescription}

object OperationKey {

  def lookup(service: ServiceDescription, key: String): Option[Operation] = {
    service.resources.flatMap(_.operations).find { op =>
      OperationKey(op).key == key
    }
  }

}

case class OperationKey(op: Operation) {

  lazy val key = lib.UrlKey.generate(s"${op.model.name} ${op.method} ${op.path}")

}
