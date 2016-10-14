package me.apidoc.swagger.translators

import lib.Primitives
import me.apidoc.swagger.{SchemaType, Util}
import com.bryzek.apidoc.spec.v0.{models => apidoc}
import com.wordnik.swagger.{models => swagger}
import com.wordnik.swagger.models.{parameters => swaggerparams}
import com.wordnik.swagger.models.{properties => swaggerproperties}

object Parameter {

  def apply(
    resolver: Resolver,
    param: swaggerparams.Parameter
  ): apidoc.Parameter = {
    // getAccess
    // getVendorExtensions

    val location = apidoc.ParameterLocation.fromString(param.getIn).getOrElse {
      sys.error(s"Could not translate param[${param.getName}] location[${param.getIn}]")
    }

    val template = apidoc.Parameter(
      name = param.getName(),
      `type` = Primitives.String.toString,
      location = location,
      description = Option(param.getDescription()),
      required = param.getRequired(),
      default = None,
      minimum = None,
      maximum = None,
      example = None
    )

    val details = param match {
      case p: swaggerparams.BodyParameter => {
        sys.error("Should never see body parameter here")
      }
      case p: swaggerparams.RefParameter => {
        sys.error("TODO: We do not support Ref Parameters")
      }
      case p: swaggerparams.CookieParameter => {
        toSchemaType(resolver, p, Option(p.getItems))
      }
      case p: swaggerparams.FormParameter => {
        toSchemaType(resolver, p, Option(p.getItems))
      }
      case p: swaggerparams.HeaderParameter => {
        toSchemaType(resolver, p, Option(p.getItems))
      }
      case p: swaggerparams.PathParameter => {
        toSchemaType(resolver, p, Option(p.getItems))
      }
      case p: swaggerparams.QueryParameter => {
        toSchemaType(resolver, p, Option(p.getItems))
      }
      case _ => {
        SchemaDetails(`type` = template.`type`)
      }
    }

    template.copy(
      `type` = details.`type`,
      description = Util.combine(Seq(template.description, details.collectionFormat.map { f => "Collection Format: $f" }))
    )
  }

  private case class SchemaDetails(
    `type`: String,
    collectionFormat: Option[String] = None
  )

  private def toSchemaType(
    resolver: Resolver,
    param: swaggerparams.SerializableParameter,
    itemProperty: Option[swaggerproperties.Property]
  ): SchemaDetails = {
    param.getType match {
      case "array" => {
        SchemaDetails(
          `type` = resolver.schemaType(
            itemProperty.getOrElse {
              sys.error("Need item property for array")
            }
          ),
          collectionFormat = Some(param.getCollectionFormat)
        )
      }
      case t => {
        SchemaDetails(
          `type` = SchemaType.fromSwaggerWithError(t, Option(param.getFormat))
        )
      }
    }
  }

}


