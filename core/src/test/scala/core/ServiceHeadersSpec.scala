package core

import org.scalatest.{FunSpec, Matchers}

class ServiceHeadersSpec extends FunSpec with Matchers
  with helpers.ServiceHelpers
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

    val validator = TestHelper.serviceValidatorFromApiJson(service, fetcher = fetcher)
    validator.errors().mkString("") should be("")
    val ct = validator.service().headers.find(_.name == "Content-Type").get
    ct.`type` should be(s"${enumsService.namespace}.enums.content_type")
  }

}
