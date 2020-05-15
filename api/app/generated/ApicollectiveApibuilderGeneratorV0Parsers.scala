/**
 * Generated by API Builder - https://www.apibuilder.io
 * Service version: 0.14.97
 * apibuilder 0.15.1 localhost 9000/apicollective/apibuilder-generator/latest/anorm_2_8_parsers
 */
import anorm._

package io.apibuilder.generator.v0.anorm.parsers {

  import io.apibuilder.generator.v0.anorm.conversions.Standard._

  import io.apibuilder.generator.v0.anorm.conversions.Types._
  import io.apibuilder.spec.v0.anorm.conversions.Types._

  object FileFlag {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.apibuilder.generator.v0.models.FileFlag] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(name: String = "file_flag", prefixOpt: Option[String] = None): RowParser[io.apibuilder.generator.v0.models.FileFlag] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) map {
        case value => io.apibuilder.generator.v0.models.FileFlag(value)
      }
    }

  }

  object Attribute {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.apibuilder.generator.v0.models.Attribute] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      name: String = "name",
      value: String = "value",
      prefixOpt: Option[String] = None
    ): RowParser[io.apibuilder.generator.v0.models.Attribute] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) ~
      SqlParser.str(prefixOpt.getOrElse("") + value) map {
        case name ~ value => {
          io.apibuilder.generator.v0.models.Attribute(
            name = name,
            value = value
          )
        }
      }
    }

  }

  object Error {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.apibuilder.generator.v0.models.Error] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      code: String = "code",
      message: String = "message",
      prefixOpt: Option[String] = None
    ): RowParser[io.apibuilder.generator.v0.models.Error] = {
      SqlParser.str(prefixOpt.getOrElse("") + code) ~
      SqlParser.str(prefixOpt.getOrElse("") + message) map {
        case code ~ message => {
          io.apibuilder.generator.v0.models.Error(
            code = code,
            message = message
          )
        }
      }
    }

  }

  object File {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.apibuilder.generator.v0.models.File] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      name: String = "name",
      dir: String = "dir",
      contents: String = "contents",
      flags: String = "flags",
      prefixOpt: Option[String] = None
    ): RowParser[io.apibuilder.generator.v0.models.File] = {
      SqlParser.str(prefixOpt.getOrElse("") + name) ~
      SqlParser.str(prefixOpt.getOrElse("") + dir).? ~
      SqlParser.str(prefixOpt.getOrElse("") + contents) ~
      SqlParser.get[Seq[io.apibuilder.generator.v0.models.FileFlag]](prefixOpt.getOrElse("") + flags).? map {
        case name ~ dir ~ contents ~ flags => {
          io.apibuilder.generator.v0.models.File(
            name = name,
            dir = dir,
            contents = contents,
            flags = flags
          )
        }
      }
    }

  }

  object Generator {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.apibuilder.generator.v0.models.Generator] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      key: String = "key",
      name: String = "name",
      language: String = "language",
      description: String = "description",
      attributes: String = "attributes",
      prefixOpt: Option[String] = None
    ): RowParser[io.apibuilder.generator.v0.models.Generator] = {
      SqlParser.str(prefixOpt.getOrElse("") + key) ~
      SqlParser.str(prefixOpt.getOrElse("") + name) ~
      SqlParser.str(prefixOpt.getOrElse("") + language).? ~
      SqlParser.str(prefixOpt.getOrElse("") + description).? ~
      SqlParser.get[Seq[String]](prefixOpt.getOrElse("") + attributes) map {
        case key ~ name ~ language ~ description ~ attributes => {
          io.apibuilder.generator.v0.models.Generator(
            key = key,
            name = name,
            language = language,
            description = description,
            attributes = attributes
          )
        }
      }
    }

  }

  object Healthcheck {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.apibuilder.generator.v0.models.Healthcheck] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      status: String = "status",
      prefixOpt: Option[String] = None
    ): RowParser[io.apibuilder.generator.v0.models.Healthcheck] = {
      SqlParser.str(prefixOpt.getOrElse("") + status) map {
        case status => {
          io.apibuilder.generator.v0.models.Healthcheck(
            status = status
          )
        }
      }
    }

  }

  object Invocation {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.apibuilder.generator.v0.models.Invocation] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      source: String = "source",
      files: String = "files",
      prefixOpt: Option[String] = None
    ): RowParser[io.apibuilder.generator.v0.models.Invocation] = {
      SqlParser.str(prefixOpt.getOrElse("") + source) ~
      SqlParser.get[Seq[io.apibuilder.generator.v0.models.File]](prefixOpt.getOrElse("") + files) map {
        case source ~ files => {
          io.apibuilder.generator.v0.models.Invocation(
            source = source,
            files = files
          )
        }
      }
    }

  }

  object InvocationForm {

    def parserWithPrefix(prefix: String, sep: String = "_"): RowParser[io.apibuilder.generator.v0.models.InvocationForm] = parser(prefixOpt = Some(s"$prefix$sep"))

    def parser(
      servicePrefix: String = "service",
      attributes: String = "attributes",
      userAgent: String = "user_agent",
      importedServices: String = "imported_services",
      prefixOpt: Option[String] = None
    ): RowParser[io.apibuilder.generator.v0.models.InvocationForm] = {
      io.apibuilder.spec.v0.anorm.parsers.Service.parserWithPrefix(prefixOpt.getOrElse("") + servicePrefix) ~
      SqlParser.get[Seq[io.apibuilder.generator.v0.models.Attribute]](prefixOpt.getOrElse("") + attributes) ~
      SqlParser.str(prefixOpt.getOrElse("") + userAgent).? ~
      SqlParser.get[Seq[io.apibuilder.spec.v0.models.Service]](prefixOpt.getOrElse("") + importedServices).? map {
        case service ~ attributes ~ userAgent ~ importedServices => {
          io.apibuilder.generator.v0.models.InvocationForm(
            service = service,
            attributes = attributes,
            userAgent = userAgent,
            importedServices = importedServices
          )
        }
      }
    }

  }

}