/**
 * Generated by API Builder - https://www.apibuilder.io
 * Service version: 0.16.50
 * User agent: apibuilder app.apibuilder.io/apicollective/apibuilder-generator/latest/play_2_8_mock_client
 */
package io.apibuilder.generator.v0.mock {

  trait Client extends io.apibuilder.generator.v0.interfaces.Client {

    val baseUrl: String = "http://mock.localhost"

    override def generators: io.apibuilder.generator.v0.Generators = MockGeneratorsImpl
    override def healthchecks: io.apibuilder.generator.v0.Healthchecks = MockHealthchecksImpl
    override def invocations: io.apibuilder.generator.v0.Invocations = MockInvocationsImpl

  }

  object MockGeneratorsImpl extends MockGenerators

  trait MockGenerators extends io.apibuilder.generator.v0.Generators {

    /**
     * Get all available generators
     *
     * @param key Filter generators with this key
     * @param limit The number of records to return
     * @param offset Used to paginate. First page of results is 0.
     */
    def get(
      key: _root_.scala.Option[String] = None,
      limit: Int = 100,
      offset: Int = 0,
      requestHeaders: Seq[(String, String)] = Nil
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[io.apibuilder.generator.v0.models.Generator]] = scala.concurrent.Future.successful {
      Nil
    }

    /**
     * Get generator with this key
     */
    def getByKey(
      key: String,
      requestHeaders: Seq[(String, String)] = Nil
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.apibuilder.generator.v0.models.Generator] = scala.concurrent.Future.successful {
      io.apibuilder.generator.v0.mock.Factories.makeGenerator()
    }

  }

  object MockHealthchecksImpl extends MockHealthchecks

  trait MockHealthchecks extends io.apibuilder.generator.v0.Healthchecks {

    def get(
      requestHeaders: Seq[(String, String)] = Nil
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.apibuilder.generator.v0.models.Healthcheck] = scala.concurrent.Future.successful {
      io.apibuilder.generator.v0.mock.Factories.makeHealthcheck()
    }

  }

  object MockInvocationsImpl extends MockInvocations

  trait MockInvocations extends io.apibuilder.generator.v0.Invocations {

    /**
     * Invoke a generator
     */
    def postByKey(
      key: String,
      invocationForm: io.apibuilder.generator.v0.models.InvocationForm,
      requestHeaders: Seq[(String, String)] = Nil
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[io.apibuilder.generator.v0.models.Invocation] = scala.concurrent.Future.successful {
      io.apibuilder.generator.v0.mock.Factories.makeInvocation()
    }

  }

  object Factories {

    def randomString(length: Int = 24): String = {
      _root_.scala.util.Random.alphanumeric.take(length).mkString
    }

    def makeFileFlag(): io.apibuilder.generator.v0.models.FileFlag = io.apibuilder.generator.v0.models.FileFlag.Scaffolding

    def makeAttribute(): io.apibuilder.generator.v0.models.Attribute = io.apibuilder.generator.v0.models.Attribute(
      name = Factories.randomString(24),
      value = Factories.randomString(24)
    )

    def makeError(): io.apibuilder.generator.v0.models.Error = io.apibuilder.generator.v0.models.Error(
      code = Factories.randomString(24),
      message = Factories.randomString(24)
    )

    def makeFile(): io.apibuilder.generator.v0.models.File = io.apibuilder.generator.v0.models.File(
      name = Factories.randomString(24),
      dir = None,
      contents = Factories.randomString(24),
      flags = None
    )

    def makeGenerator(): io.apibuilder.generator.v0.models.Generator = io.apibuilder.generator.v0.models.Generator(
      key = Factories.randomString(24),
      name = Factories.randomString(24),
      language = None,
      description = None,
      attributes = Nil
    )

    def makeHealthcheck(): io.apibuilder.generator.v0.models.Healthcheck = io.apibuilder.generator.v0.models.Healthcheck(
      status = Factories.randomString(24)
    )

    def makeInvocation(): io.apibuilder.generator.v0.models.Invocation = io.apibuilder.generator.v0.models.Invocation(
      source = Factories.randomString(24),
      files = Nil
    )

    def makeInvocationForm(): io.apibuilder.generator.v0.models.InvocationForm = io.apibuilder.generator.v0.models.InvocationForm(
      service = io.apibuilder.spec.v0.mock.Factories.makeService(),
      attributes = Nil,
      userAgent = None,
      importedServices = None
    )

  }

}