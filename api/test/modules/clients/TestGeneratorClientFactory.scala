package modules.clients

import io.apibuilder.generator.v0.interfaces.Client
import io.apibuilder.generator.v0.mock.{MockGenerators, Client => MockClient}
import io.apibuilder.generator.v0.models.Generator

import javax.inject.{Inject, Singleton}
import scala.collection.concurrent.TrieMap
import scala.concurrent.{ExecutionContext, Future}

class TestGeneratorClientFactory @Inject() (
  data: MockGeneratorsData
) extends GeneratorClientFactory {

  def instance(baseUrl: String): Client = MockGeneratorsClient(
    generators = LocalMockGenerators(
      baseUrl = baseUrl,
      data = data,
    )
  )

}

case class MockGeneratorsClient (
  override val generators: LocalMockGenerators
) extends MockClient

@Singleton
class MockGeneratorsData @Inject() () {
  private[this] case class InternalCacheKey(baseUrl: String, generatorKey: String)
  private[this] val data = TrieMap[InternalCacheKey, Generator]()

  def add(baseUrl: String, generator: Generator): Unit = {
    data.put(InternalCacheKey(baseUrl, generator.key), generator)
  }
  def getAll(baseUrl: String): Seq[Generator] = {
    data.filter { case (k, v) =>
      k.baseUrl == baseUrl
    }.values.toSeq
  }
}

case class LocalMockGenerators(
  baseUrl: String,
  data: MockGeneratorsData
) extends MockGenerators {
  override def get(
    key: _root_.scala.Option[String] = None,
    limit: Int = 100,
    offset: Int = 0,
    requestHeaders: Seq[(String, String)] = Nil
  )(implicit ec: ExecutionContext): Future[Seq[Generator]] = Future {
    data
      .getAll(baseUrl)
      .filter { g => key.isEmpty || key.contains(g.key) }
      .take(limit)
      .drop(offset)
  }
}
