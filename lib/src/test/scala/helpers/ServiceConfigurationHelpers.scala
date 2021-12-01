package helpers

import java.util.UUID
import lib.ServiceConfiguration

trait ServiceConfigurationHelpers {

  def makeServiceConfiguration(
    orgNamespace: String = UUID.randomUUID.toString,
  ): ServiceConfiguration = {
    ServiceConfiguration(
      orgKey = "apidoc",
      orgNamespace = orgNamespace,
      version = "1.0"
    )
  }

}
