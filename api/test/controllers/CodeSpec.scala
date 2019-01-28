package controllers

import com.github.tomakehurst.wiremock.WireMockServer
import io.apibuilder.spec.v0.models.Import
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import io.apibuilder.spec.v0.{models => spec}
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.matching.RequestPattern
import io.apibuilder.generator.v0.models.{Invocation, InvocationForm}
import io.apibuilder.generator.v0.models.json.{jsonReadsApibuilderGeneratorInvocationForm, jsonWritesApibuilderGeneratorInvocation}
import play.api.libs.json.Json

import scala.util.Try

class CodeSpec extends PlaySpec with MockClient with GuiceOneServerPerSuite with db.generators.Helpers {

  import scala.concurrent.ExecutionContext.Implicits.global

  "Code controller" should {

    "post payload containing imported services to generator" in {

      val generatorWithService = generatorsDao.findAll(db.Authorization.All)
        .headOption.getOrElse(throw new IllegalArgumentException("At least one code generator expected in database"))

      val generatorPort = generatorWithService.service.uri.dropWhile(_ != ':').drop(1).toInt
      val generatorKey = generatorWithService.generator.key

      val wireMockServer = new WireMockServer(generatorPort)

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

        val resultF = client.code.get(testOrg.key, mainApp.key, mainVersion.version, generatorKey)

        expectStatus(200) {
          resultF.map(_ => ())
        }

        val result = await(resultF)
        result.files mustEqual mockGeneratorFileList
        result.generator mustEqual generatorWithService

        val wireMockRequestList = wireMockServer.findRequestsMatching(RequestPattern.ANYTHING).getRequests
        val postRequestBodyString = Try(wireMockRequestList.get(0))
          .getOrElse(throw new IllegalArgumentException("WireMock test server has not captured any Apibuilder Generator invocation call"))
          .getBodyAsString
        val sentInvocationForm = Json.parse(postRequestBodyString).as[InvocationForm]

        sentInvocationForm.importedServices mustEqual Some(Seq(intermediateService, childService))

      } finally {
        wireMockServer.stop()
      }
    }

  }

}
