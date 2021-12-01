package io.apibuilder.swagger

import java.util

import io.apibuilder.spec.v0.models.Attribute
import io.swagger.models.SecurityRequirement
import io.swagger.models.auth.SecuritySchemeDefinition
import org.scalatest.{FunSpec, Matchers}
import play.api.libs.json.{JsObject, JsString}

class SwaggerDataSpec extends FunSpec with Matchers {

  it("all data points empty/null") {
    SwaggerData().toAttribute should be (None)
    SwaggerData(
      serviceSecurity = new util.ArrayList[SecurityRequirement](),
      operationSecurity = new util.ArrayList[util.Map[String, util.List[String]]](),
      securityDefinitions = new util.HashMap[String, SecuritySchemeDefinition]()
    ).toAttribute should be (None)
  }

  it("one data point present") {
    SwaggerData(
      example = "Example object"
    ).toAttribute should be(
      Some( Attribute(
        name = SwaggerData.AttributeName,
        value = JsObject(Seq(
          ("example", JsString("Example object")))),
        description = Some(SwaggerData.AttributeDescription)
      )))
  }
}
