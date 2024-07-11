package io.apibuilder.avro

import java.io.File
import lib.ServiceConfiguration
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ParserSpec extends AnyFunSpec with Matchers {

  val dir = "avro/src/test/resources/me/apidoc/avro"
  val config = ServiceConfiguration(
    orgKey = "gilt",
    orgNamespace = "io.apibuilder",
    version = "0.0.1-dev"
  )

  it("parses") {
    //Parser(config).parse(s"$dir/mobile-tapstream.avpr")
    //Parser(config).parse(s"$dir/simple-protocol.avpr")

    //Parser(config).parse(s"$dir/gfc-avro.avdl")
    //Parser(config).parse(s"$dir/simple-protocol-with-gfc.avpr")
    // Parser(config).parse(s"$dir/mobile-tapstream.avpr")
    val service = Parser(config).parse(new File("avro/example.avdl"))
    println("name: " + service.name)
    println("namespace: " + service.namespace)

    println("Enums:")
    service.enums.foreach { e =>
      println(s" - ${e.name}")
      e.values.foreach { value =>
        println(s"   - ${value.name}")
      }
    }

    println("Models:")
    service.models.foreach { model =>
      println(s" - ${model.name}")
      model.fields.foreach { field =>
        println(s"   - ${field}")
      }
    }

    println("Unions:")
    service.unions.foreach { union =>
      println(s" - ${union.name} (${union.description})")
      union.types.foreach { t =>
        println(s"   - ${t}")
      }
    }
  }

}
