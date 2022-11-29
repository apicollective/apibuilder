package core

import helpers.{ServiceHelpers, ValidatedTestHelpers}
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ServiceHeadersSpec extends AnyFunSpec with Matchers
  with ServiceHelpers
  with ValidatedTestHelpers
{

  it("headers can reference imported enums") {
    val enumsService = makeService(
      enums = Seq(
        makeEnum(
          name = "content_type",
          values = Seq(makeEnumValue("application/json"))
        )
      )
    )

    val service = s"""
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },
      "imports": [
        { "uri": "${makeImportUri(enumsService)}" }
      ],

      "headers": [
        { "name": "Content-Type", "type": "${enumsService.namespace}.enums.content_type" }
      ]
    }
    """

    val fetcher = MockServiceFetcher()
    fetcher.add(makeImportUri(enumsService), enumsService)

    expectValid {
      TestHelper.serviceValidatorFromApiJson(service, fetcher = fetcher)
    }.headers.find(_.name == "Content-Type").get.`type` should be(s"${enumsService.namespace}.enums.content_type")
  }

}
