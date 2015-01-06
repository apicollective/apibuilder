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
    res.orgNamespace should be("com.gilt")
    res.applicationKey should be("apidoc")
    res.kind should be(TypeKind.Model)
    res.name should be("application")
  }

  it("resolves enums") {
    TypeNameResolver("age_group").resolve should be(None)
    TypeNameResolver("com.gilt.apidoc.enums").resolve should be(None)
    TypeNameResolver("com.gilt.apidoc.age_group").resolve should be(None)
    TypeNameResolver("com.gilt.apidoc.enum.age_group").resolve should be(None)

    val res = TypeNameResolver("com.gilt.apidoc.enums.age_group").resolve.getOrElse {
      sys.error("Failed to resolve")
    }
    res.orgNamespace should be("com.gilt")
    res.kind should be(TypeKind.Enum)
    res.applicationKey should be("apidoc")
    res.name should be("age_group")
  }

}
