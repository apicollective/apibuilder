package controllers

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.matching.RequestPattern
import io.apibuilder.api.v0.models.CodeForm
import io.apibuilder.generator.v0.models.json.{jsonReadsApibuilderGeneratorInvocationForm, jsonWritesApibuilderGeneratorInvocation}
import io.apibuilder.generator.v0.models.{Attribute, Invocation, InvocationForm}
import io.apibuilder.spec.v0.models.Import
import io.apibuilder.spec.v0.{models => spec}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import util.RandomPortFinder

import scala.util.Try

class CodeSpec extends PlaySpec with MockClient with GuiceOneServerPerSuite with db.generators.GeneratorHelpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  private[this] def makeCodeForm(attributes: Seq[Attribute] = Nil) = CodeForm(
    attributes = attributes
  )

  private[this] def makeAttribute(): Attribute = {
    Attribute(
      name = randomString(),
      value = randomString()
    )
  }

  "Code controller" should {

    "postForm returns invocation form" in {
      val org = createOrganization()
      val app = createApplication(org)
      val version = createVersion(app)
      val attributes = Seq(makeAttribute())

      val form = await {
        client.code.postForm(
          orgKey = org.key,
          applicationKey = app.key,
          version = version.version,
          codeForm = makeCodeForm(attributes = attributes)
        )
      }
      form.service.name must equal(version.service.name)
      form.userAgent.isDefined must be(true)
      form.attributes must equal(attributes)
    }

    "post payload containing imported services to generator" in {

      val randomPort = RandomPortFinder.getRandomPort
      val generatorWithService = createGenerator(createGeneratorService(createGeneratorServiceForm(s"http://localhost:$randomPort")))

      val generatorKey = generatorWithService.generator.key

      val wireMockServer = new WireMockServer(randomPort)

      try {
        wireMockServer.start()

        val mockGeneratorFileList = Seq.empty

        wireMockServer.stubFor(
          post(urlEqualTo(s"/invocations/$generatorKey"))
            .willReturn(okJson(
              Json.toJson(Invocation("", mockGeneratorFileList)).toString()
            ))
        )

        val testOrg = createOrganization()
        // create the first service that will be referenced by an import of an another import
        val childApp = createApplication(org = testOrg)
        val childService = createService(childApp)
        val childVersion = createVersion(application = childApp, service = Some(childService))

        // create the second service that will reference the first one and will be referenced by an import inside main service
        val intermediateApp = createApplication(org = testOrg)
        val intermediateService = createService(intermediateApp).copy(
          imports = Seq(
            Import(uri = "irrelevant", namespace = childService.namespace, organization = spec.Organization(testOrg.key), application = spec.Application(childApp.key), version = childVersion.version)
          )
        )
        val intermediateVersion = createVersion(application = intermediateApp, service = Some(intermediateService))

        // create the main service that will import the second one
        val mainApp = createApplication(org = testOrg)
        val mainService = createService(mainApp).copy(
          imports = Seq(
            Import(uri = "anything", namespace = intermediateService.namespace, organization = spec.Organization(testOrg.key), application = spec.Application(intermediateApp.key), version = intermediateVersion.version)
          )
        )
        val mainVersion = createVersion(application = mainApp, service = Some(mainService))

        val resultF = client.code.getByGeneratorKey(testOrg.key, mainApp.key, mainVersion.version, generatorKey)

        expectStatus(200) {
          resultF.map(_ => ())
        }

        val result = await(resultF)
        result.files mustEqual mockGeneratorFileList
        result.generator mustEqual generatorWithService

        val wireMockRequestList = wireMockServer.findRequestsMatching(RequestPattern.ANYTHING).getRequests
        val postRequestBodyString = Try(wireMockRequestList.get(0))
          .getOrElse(throw new IllegalArgumentException("WireMock test server has not captured any ApiBuilder Generator invocation call"))
          .getBodyAsString
        val sentInvocationForm = Json.parse(postRequestBodyString).as[InvocationForm]

        sentInvocationForm.importedServices.getOrElse(Nil).map(_.namespace).sorted mustEqual Seq(
          intermediateService, childService
        ).map(_.namespace).sorted

      } finally {
        wireMockServer.stop()
      }
    }

  }

}
