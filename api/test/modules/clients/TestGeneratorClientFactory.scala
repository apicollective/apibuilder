package modules.clients

import io.apibuilder.generator.v0.interfaces.Client
import io.apibuilder.generator.v0.mock.{MockGenerators, Client => MockClient}

import javax.inject.{Inject, Singleton}

class TestGeneratorClientFactory @Inject() (
  client: MockGeneratorsClient
) extends GeneratorClientFactory {

  def instance(baseUrl: String): Client = client

}

class MockGeneratorsClient @Inject() (
  override val generators: LocalMockGenerators
) extends MockClient

@Singleton
class MockGeneratorsData @Inject() () {

}

class LocalMockGenerators @Inject() (
  data: MockGeneratorsData
) extends MockGenerators {

}
