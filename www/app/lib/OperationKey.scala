package lib

import com.gilt.apidoc.spec.v0.models.{Model, Operation, Resource, Service}

object OperationKey {

  def lookup(service: Service, key: String): Option[Operation] = {
    service.resources.flatMap { resource =>
      resource.operations.find { op =>
        OperationKey(resource, op).key == key
      }
    }.headOption
  }

}

case class OperationKey(
  resource: Resource,
  op: Operation
) {

  lazy val key = lib.UrlKey.generate(s"${resource.model.name} ${op.method} ${op.path}")

}
