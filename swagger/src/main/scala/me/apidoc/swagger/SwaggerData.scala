package me.apidoc.swagger

import com.bryzek.apidoc.spec.v0.models.Attribute
import io.swagger.models.auth.SecuritySchemeDefinition
import io.swagger.models.{ExternalDocs, SecurityRequirement}
import me.apidoc.swagger.SwaggerData._
import play.api.libs.json.JsObject

case class SwaggerData(
                        externalDocs: ExternalDocs = null,
                        serviceSecurity: java.util.List[SecurityRequirement]  = null,
                        operationSecurity: java.util.List[java.util.Map[String, java.util.List[String]]] = null,
                        securityDefinitions: java.util.Map[String, SecuritySchemeDefinition] = null,
                        example: Object = null) {

  def toAttribute: Option[Attribute] = {
    val jsObjFields =
      Seq(
        Option(externalDocs).map { externalDocs =>
          (ExternalDocsKeyName, Util.toJsValue(externalDocs))
        },
        Option(serviceSecurity).flatMap { serviceSecurity =>
          if(serviceSecurity.isEmpty) None else Some((SecurityKeyName, Util.toJsValue(serviceSecurity)))
        },
        Option(operationSecurity).flatMap { operationSecurity =>
          if(operationSecurity.isEmpty) None else Some((SecurityKeyName, Util.toJsValue(operationSecurity)))
        },
        Option(securityDefinitions).flatMap { securityDefinitions =>
          if(securityDefinitions.isEmpty) None else Some((SecurityDefinitionsKeyName, Util.toJsValue(securityDefinitions)))
        },
        Option(example).map { example =>
          (ExampleKeyName, Util.toJsValue(example))
        }
      ).flatten

    if(jsObjFields.isEmpty){
      None
    } else {
      Some(Attribute(
        name = AttributeName,
        value = JsObject(jsObjFields),
        description = Some(AttributeDescription),
        deprecation = None))
    }
  }
}

object SwaggerData {
  val AttributeName = "swagger"
  val AttributeDescription = "Swagger-specific data"

  val ExternalDocsKeyName = "externalDocs"
  val SecurityKeyName = "security"
  val SecurityDefinitionsKeyName = "securityDefinitions"
  val ExampleKeyName = "example"
}
