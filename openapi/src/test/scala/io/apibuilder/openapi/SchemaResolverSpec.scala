package io.apibuilder.openapi

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SchemaResolverSpec extends AnyWordSpec with Matchers {

  import SchemaResolver._

  "refName" must {

    "strip the #/components/schemas/ prefix" in {
      refName("#/components/schemas/Foo") must be("Foo")
    }

    "return the raw string for a non-standard ref" in {
      refName("./other.yaml#/Foo") must be("./other.yaml#/Foo")
    }
  }

  "resolveReference" must {

    "resolve a direct alias" in {
      val refs = Map("MyAlias" -> "Widget")
      resolveReference("MyAlias", refs) must be(Right("Widget"))
    }

    "resolve a chain of aliases" in {
      val refs = Map("A" -> "B", "B" -> "C")
      resolveReference("A", refs) must be(Right("C"))
    }

    "return Right(name) when name is not in refs" in {
      resolveReference("Unknown", Map.empty) must be(Right("Unknown"))
    }

    "detect a cycle and return Left" in {
      val refs = Map("A" -> "B", "B" -> "A")
      val result = resolveReference("A", refs)
      result must be(Symbol("left"))
      result.left.get must include("Cycle detected")
    }
  }
}
