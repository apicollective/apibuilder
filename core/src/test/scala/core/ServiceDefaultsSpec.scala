package core

import org.scalatest.{FunSpec, Matchers}

class ServiceDefaultsSpec extends FunSpec with Matchers {

  it("accepts defaults for date-iso8601") {
    val json = """
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },
      "models": {
        "user": {
          "fields": [
            { "name": "created_at", "type": "date-iso8601", "default": "2014-01-01" }
          ]
        }
      }
    }
    """
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors().mkString("") should be("")

    val createdAt = validator.service().models.head.fields.find { _.name == "created_at" }.get
    createdAt.default should be(Some("2014-01-01"))
  }

  it("accepts strings and values as defaults for booleans") {
    val json = """
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },
      "models": {
        "user": {
          "fields": [
            { "name": "is_active", "type": "boolean", "default": "true", "required": true },
            { "name": "is_athlete", "type": "boolean", "default": "false", "required": "true" }
          ]
        }
      }
    }
    """
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors().mkString("") should be("")

    val isActiveField = validator.service().models.head.fields.find { _.name == "is_active" }.get
    isActiveField.default should be(Some("true"))
    isActiveField.required should be(true)

    val isAthleteField = validator.service().models.head.fields.find { _.name == "is_athlete" }.get
    isAthleteField.default should be(Some("false"))
    isAthleteField.required should be(true)
  }

  it("rejects invalid boolean defaults") {
    val json = """
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },
      "models": {
        "user": {
          "fields": [
            { "name": "is_active", "type": "boolean", "default": 1 }
          ]
        }
      }
    }
    """
    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors().mkString("") should be("Model[user] Field[is_active] Value[1] is not a valid boolean. Must be one of: true, false")
  }

  it("fields with defaults may be marked optional") {
    val jsonOpt =
      """
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },

      "models": {
        "user": {
          "fields": [
            { "name": "created_at", "type": "date-iso8601", "default": "2014-01-01", "required": false }
          ]
        }
      }
    }
    """
    val validator = TestHelper.serviceValidatorFromApiJson(jsonOpt)
    validator.errors().mkString("") should be("")
  }

  it("fields with defaults may be marked required") {
    val jsonReq = """
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },

      "models": {
        "user": {
          "fields": [
            { "name": "created_at", "type": "date-iso8601", "default": "2014-01-01", "required": true }
          ]
        }
      }
    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(jsonReq)
    validator.errors().mkString("") should be("")
  }

  def buildMinMaxErrors(
    default: Number,
    min: Option[Long] = None,
    max: Option[Long] = None
  ): Seq[String] = {
    val props = Seq(
      Some(s""""default": "$default""""),
      min.map { v => s""""minimum": "$v"""" },
      max.map { v => s""""maximum": "$v"""" }
    ).flatten.mkString(", ", ", ", "")

    val json = s"""
    {
      "name": "API Builder",
      "models": {
        "user": {
          "fields": [
            { "name": "age", "type": "long"$props }
          ]
        }
      }
    }
    """
    TestHelper.serviceValidatorFromApiJson(json).errors()
  }

  it("numeric default is in between min, max") {
    buildMinMaxErrors(1) should be(Nil)

    buildMinMaxErrors(1, min = Some(0)) should be(Nil)
    buildMinMaxErrors(1, min = Some(1)) should be(Nil)

    buildMinMaxErrors(1, max = Some(1)) should be(Nil)
    buildMinMaxErrors(1, max = Some(2)) should be(Nil)
  }

  it("numeric default ourtide of min, max") {
    buildMinMaxErrors(1, min = Some(2)) should be(
      Seq("user.age default[1] must be >= specified minimum[2]")
    )

    buildMinMaxErrors(1000, max = Some(100)) should be(
      Seq("user.age default[1000] must be <= specified maximum[100]")
    )
  }

}
