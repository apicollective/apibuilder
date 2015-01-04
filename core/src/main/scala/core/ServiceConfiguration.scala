package core

import com.gilt.apidoc.models.Organization

case class ServiceConfiguration(
  orgNamespace: String
)


object ServiceConfiguration {

  def apply(org: Organization): ServiceConfiguration = {
    ServiceConfiguration(
      orgNamespace = org.namespace
    )
  }

}
