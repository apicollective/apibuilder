package core

import com.gilt.apidoc.models.Organization

case class ServiceConfiguration(
  orgNamespace: String,
  version: String
)


object ServiceConfiguration {

  def apply(
    org: Organization,
    version: String
  ): ServiceConfiguration = {
    ServiceConfiguration(
      orgNamespace = org.namespace,
      version = version
    )
  }

}
