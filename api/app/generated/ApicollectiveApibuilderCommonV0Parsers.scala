/**
 * Generated by API Builder - https://www.apibuilder.io
 * Service version: 0.12.24
 * apibuilder:0.12.38 https://app.apibuilder.io/apicollective/apibuilder-common/0.12.24/anorm_2_x_parsers
 */
import anorm._

package io.apibuilder.common.v0.anorm.parsers {

  import io.apibuilder.common.v0.anorm.conversions.Standard._

  import io.apibuilder.common.v0.anorm.conversions.Types._

  object Audit {

    def parserWithPrefix(prefix: String, sep: String = "_") = parser(
      createdAt = s"$prefix${sep}created_at",
      createdByPrefix = s"$prefix${sep}created_by",
      updatedAt = s"$prefix${sep}updated_at",
      updatedByPrefix = s"$prefix${sep}updated_by"
    )

    def parser(
      createdAt: String = "created_at",
      createdByPrefix: String = "created_by",
      updatedAt: String = "updated_at",
      updatedByPrefix: String = "updated_by"
    ): RowParser[io.apibuilder.common.v0.models.Audit] = {
      SqlParser.get[_root_.org.joda.time.DateTime](createdAt) ~
      io.apibuilder.common.v0.anorm.parsers.ReferenceGuid.parserWithPrefix(createdByPrefix) ~
      SqlParser.get[_root_.org.joda.time.DateTime](updatedAt) ~
      io.apibuilder.common.v0.anorm.parsers.ReferenceGuid.parserWithPrefix(updatedByPrefix) map {
        case createdAt ~ createdBy ~ updatedAt ~ updatedBy => {
          io.apibuilder.common.v0.models.Audit(
            createdAt = createdAt,
            createdBy = createdBy,
            updatedAt = updatedAt,
            updatedBy = updatedBy
          )
        }
      }
    }

  }

  object Healthcheck {

    def parserWithPrefix(prefix: String, sep: String = "_") = parser(
      status = s"$prefix${sep}status"
    )

    def parser(
      status: String = "status"
    ): RowParser[io.apibuilder.common.v0.models.Healthcheck] = {
      SqlParser.str(status) map {
        case status => {
          io.apibuilder.common.v0.models.Healthcheck(
            status = status
          )
        }
      }
    }

  }

  object Reference {

    def parserWithPrefix(prefix: String, sep: String = "_") = parser(
      guid = s"$prefix${sep}guid",
      key = s"$prefix${sep}key"
    )

    def parser(
      guid: String = "guid",
      key: String = "key"
    ): RowParser[io.apibuilder.common.v0.models.Reference] = {
      SqlParser.get[_root_.java.util.UUID](guid) ~
      SqlParser.str(key) map {
        case guid ~ key => {
          io.apibuilder.common.v0.models.Reference(
            guid = guid,
            key = key
          )
        }
      }
    }

  }

  object ReferenceGuid {

    def parserWithPrefix(prefix: String, sep: String = "_") = parser(
      guid = s"$prefix${sep}guid"
    )

    def parser(
      guid: String = "guid"
    ): RowParser[io.apibuilder.common.v0.models.ReferenceGuid] = {
      SqlParser.get[_root_.java.util.UUID](guid) map {
        case guid => {
          io.apibuilder.common.v0.models.ReferenceGuid(
            guid = guid
          )
        }
      }
    }

  }

}