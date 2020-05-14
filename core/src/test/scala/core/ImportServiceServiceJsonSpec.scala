package core

import builder.OriginalValidator
import io.apibuilder.api.json.v0.models.Model
import io.apibuilder.api.json.v0.models.json._
import io.apibuilder.spec.v0.models.json._
import io.apibuilder.api.v0.models.{Original, OriginalType}
import org.scalatest.{FunSpec, Matchers}
import play.api.libs.json.Json

class ImportServiceServiceJsonSpec extends FunSpec with Matchers with helpers.ApiJsonHelpers {

  private[this] def makeUserModel(idType: String = "long"): Model = {
    makeModel(
      fields = Seq(makeField(name = "id", `type` = idType)),
    )
  }

  describe("with valid service") {
    val importSharedService = toService(
      makeApiJson(
        name = "Import Shared",
        namespace = Some("test.apibuilder.import-shared"),
        enums = Map(
          "age_group" -> makeEnum(
            plural = Some("age_groups"),
            values = Seq(
              makeEnumValue(name = "youth"),
              makeEnumValue(name = "adult"),
            )
          )
        ),
        unions = Map(
          "user_or_guest" -> makeUnion(
            plural = Some("user_or_guests"),
            types = Seq(
              makeUnionType(`type` = "user"),
              makeUnionType(`type` = "guest"),
            )
          ),
          "user_or_random" -> makeUnion(
            plural = Some("user_or_randoms"),
            types = Seq(
              makeUnionType(`type` = "user"),
              makeUnionType(`type` = "random_user"),
            )
          )
        ),
        models = Map(
          "user" -> makeUserModel(),
          "guest" -> makeUserModel(),
          "random_user" -> makeUserModel(idType = "uuid"),
        )
      )
    )

    val json1File = TestHelper.writeToTempFile(
      Json.toJson(importSharedService).toString
    )

    val importServiceApiJson = makeApiJson(
      name = "Import Service",
      imports = Seq(makeImport(uri = s"file://$json1File")),
      models = Map(
        "membership" -> makeModel(
          fields = Seq(
            makeField("id", "long"),
            makeField("user", "test.apibuilder.import-shared.models.user"),
            makeField("age_group", "test.apibuilder.import-shared.enums.age_group"),
          )
        ),
      ),
      resources = Map(
        "test.apibuilder.import-shared.models.user" -> makeResource(
          operations = Seq(makeOperation(method = "GET", path = "/get/:id"))
        ),
        "test.apibuilder.import-shared.unions.user_or_guest" -> makeResource(
          operations = Seq(makeOperation(method = "GET", path = "/get/:id"))
        ),
        "test.apibuilder.import-shared.unions.user_or_random" -> makeResource(
          operations = Seq(makeOperation(method = "GET", path = "/get/:id"))
        ),
      )
    )

    lazy val validator = OriginalValidator(
      config = TestHelper.serviceConfig,
      original = Original(OriginalType.ApiJson, Json.toJson(importServiceApiJson).toString),
      fetcher = FileServiceFetcher(),
    )

    lazy val validService = validator.validate match {
      case Left(errors) => sys.error(errors.mkString(","))
      case Right(service) => service
    }

    it("parses service definition with imports") {
      validator.validate match {
        case Left(errors) => {
          fail(errors.mkString(""))
        }
        case Right(_) => {
          // Success
        }
      }
    }

    it("infers datatype for an imported field") {
      val resource = validService.resources.find(_.`type` == "test.apibuilder.import-shared.models.user").getOrElse {
        sys.error("Could not find resource")
      }
      resource.operations.head.parameters.find(_.name == "id").getOrElse {
        fail("Could not find parameter named[id]")
      }.`type` should be("long")
    }

    it("infers datatype for an imported field from a union type") {
      val resource = validService.resources.find(_.`type` == "test.apibuilder.import-shared.unions.user_or_guest").getOrElse {
        sys.error("Could not find resource")
      }
      resource.operations.head.parameters.find(_.name == "id").getOrElse {
        fail("Could not find parameter named[id]")
      }.`type` should be("long")
    }

    it("defaults datatype to string when type varies across union types") {
      val resource = validService.resources.find(_.`type` == "test.apibuilder.import-shared.unions.user_or_random").getOrElse {
        sys.error("Could not find resource")
      }
      resource.operations.head.parameters.find(_.name == "id").getOrElse {
        fail("Could not find parameter named[id]")
      }.`type` should be("string")
    }

  }
}
