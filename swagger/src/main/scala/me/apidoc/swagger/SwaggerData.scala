package me.apidoc.swagger

import io.apibuilder.spec.v0.models.Attribute
import io.swagger.models.auth.SecuritySchemeDefinition
import io.swagger.models.{ExternalDocs, Scheme, SecurityRequirement}
import me.apidoc.swagger.SwaggerData._
import play.api.libs.json.JsObject

case class SwaggerData(
                        externalDocs: ExternalDocs = null,
                        schemes: java.util.List[Scheme] = null,
                        host: String = null,
                        basePath: String = null,
                        serviceSecurity: java.util.List[SecurityRequirement]  = null,
                        operationSecurity: java.util.List[java.util.Map[String, java.util.List[String]]] = null,
                        securityDefinitions: java.util.Map[String, SecuritySchemeDefinition] = null,
                        example: Object = null,
                        operationId: String = null,
                        tags: java.util.List[String] = null,
                        summary: String = null
                      ) {

  def toAttribute: Option[Attribute] = {
    val jsObjFields =
      Seq(
        Option(externalDocs).map { externalDocs =>
          (ExternalDocsKeyName, Util.toJsValue(externalDocs))
        },
        Option(schemes).flatMap { schemes =>
          if(schemes.isEmpty) None else Some((SchemesKeyName, Util.toJsValue(schemes)))
        },
        Option(host).map { host =>
          (HostKeyName, Util.toJsValue(host))
        },
        Option(basePath).map { basePath =>
          (BasePathKeyName, Util.toJsValue(basePath))
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
        },
        Option(tags).flatMap { tags =>
          if(tags.isEmpty) None else Some((TagsKeyName, Util.toJsValue(tags)))
        },
        Option(summary).map { summary =>
          (SummaryKeyName, Util.toJsValue(summary))
        },
        Option(operationId).map { operationId =>
          (OperationIdKeyName, Util.toJsValue(operationId))
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
  val SchemesKeyName = "schemes"
  val HostKeyName = "host"
  val BasePathKeyName = "basePath"
  val OperationIdKeyName = "operationId"
  val SummaryKeyName = "summary"
  val TagsKeyName = "tags"
}
