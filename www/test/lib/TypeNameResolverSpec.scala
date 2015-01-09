package lib

import org.scalatest.{FunSpec, ShouldMatchers}

class TypeNameResolverSpec extends FunSpec with ShouldMatchers {

  it("resolves models") {
    TypeNameResolver("service").resolve should be(None)
    TypeNameResolver("com.gilt.apidoc.models").resolve should be(None)
    TypeNameResolver("com.gilt.apidoc.service").resolve should be(None)
    TypeNameResolver("com.gilt.apidoc.model.service").resolve should be(None)

    val res = TypeNameResolver("com.gilt.apidoc.models.service").resolve.getOrElse {
      sys.error("Failed to resolve")
    }
    res.namespace should be("com.gilt.apidoc")
    res.kind should be(Kind.Model)
    res.name should be("service")
  }

  it("resolves enums") {
    TypeNameResolver("age_group").resolve should be(None)
    TypeNameResolver("com.gilt.apidoc.enums").resolve should be(None)
    TypeNameResolver("com.gilt.apidoc.age_group").resolve should be(None)
    TypeNameResolver("com.gilt.apidoc.enum.age_group").resolve should be(None)

    val res = TypeNameResolver("com.gilt.apidoc.enums.age_group").resolve.getOrElse {
      sys.error("Failed to resolve")
    }
    res.namespace should be("com.gilt.apidoc")
    res.kind should be(Kind.Enum)
    res.name should be("age_group")
  }

}
