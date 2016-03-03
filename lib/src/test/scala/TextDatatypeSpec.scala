package lib

import org.scalatest.{FunSpec, Matchers}

class TextDatatypeSpec extends FunSpec with Matchers {

  def label(types: Seq[TextDatatype]): String = {
    types.reverse match {
      case Nil => ""
      case TextDatatype.Singleton(name) :: rest => label(name, rest)
      case _ => sys.error(s"Could not parse $types")
    }
  }

  def label(result: String, types: Seq[TextDatatype]): String = {
    types match {
      case Nil => result
      case one :: rest => {
        one match {
          case TextDatatype.List => label(s"[$result]", types.drop(1))
          case TextDatatype.Map => label(s"map[$result]", types.drop(1))
          case TextDatatype.Singleton(name) => sys.error("Invalid")
        }
      }
    }
  }

  it("primitives") {
    label(TextDatatype.parse("string")) should be("string")
    label(TextDatatype.parse("long")) should be("long")
    label(TextDatatype.parse("uuid")) should be("uuid")
    label(TextDatatype.parse("unit")) should be("unit")
    label(TextDatatype.parse("integer")) should be("integer")
    label(TextDatatype.parse("date-time-iso8601")) should be("date-time-iso8601")

    label(TextDatatype.parse("[string]")) should be("[string]")
    label(TextDatatype.parse("[long]")) should be("[long]")
    label(TextDatatype.parse("[uuid]")) should be("[uuid]")
    label(TextDatatype.parse("[unit]")) should be("[unit]")
    label(TextDatatype.parse("[integer]")) should be("[integer]")
    label(TextDatatype.parse("[date-time-iso8601]")) should be("[date-time-iso8601]")

    label(TextDatatype.parse("map")) should be("map[string]")
    label(TextDatatype.parse("map[string]")) should be("map[string]")
    label(TextDatatype.parse("map[long]")) should be("map[long]")
    label(TextDatatype.parse("map[uuid]")) should be("map[uuid]")
    label(TextDatatype.parse("map[unit]")) should be("map[unit]")
    label(TextDatatype.parse("map[integer]")) should be("map[integer]")
    label(TextDatatype.parse("map[date-time-iso8601]")) should be("map[date-time-iso8601]")

    label(TextDatatype.parse("map[[string]]")) should be("map[[string]]")
    label(TextDatatype.parse("map[map[string]]")) should be("map[map[string]]")
    label(TextDatatype.parse("map[map[[user]]]")) should be("map[map[[user]]]")
    
    label(TextDatatype.parse("user")) should be("user")
  }

}
