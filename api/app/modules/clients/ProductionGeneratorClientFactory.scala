package modules.clients

import io.apibuilder.generator.v0.interfaces.{Client => ClientInterface}
import io.apibuilder.generator.v0.{Client => GeneratorClient}
import play.api.libs.ws.WSClient

import javax.inject.Inject

trait GeneratorClientFactory {
  def instance(baseUrl: String): ClientInterface
}

class ProductionGeneratorClientFactory @Inject() (
  ws: WSClient
) extends GeneratorClientFactory {

  def instance(baseUrl: String): GeneratorClient = {
    new GeneratorClient(
      ws = ws,
      baseUrl = baseUrl
    )
  }

}
