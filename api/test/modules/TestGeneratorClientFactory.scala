package modules.clients

import io.apibuilder.generator.v0.mock.{Client => MockClient, MockGenerators}
import io.apibuilder.generator.v0.interfaces.Client

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
