package me.apidoc.avro

import org.scalatest.{FunSpec, Matchers}

class ParserSpec extends FunSpec with Matchers {

  val dir = "avro/src/test/resources/me/apidoc/avro"

  it("parses") {
    //Parser().parse(s"$dir/mobile-tapstream.avpr")
    //Parser().parse(s"$dir/simple-protocol.avpr")

    //Parser().parse(s"$dir/gfc-avro.avdl")
    //Parser().parse(s"$dir/simple-protocol-with-gfc.avpr")
    // Parser().parse(s"$dir/mobile-tapstream.avpr")
    val service = Parser().parse(s"avro/example.avdl")
    println("name: " + service.name)
    println("namespace: " + service.namespace)

    println("Enums:")
    service.enums.foreach { enum =>
      println(s" - ${enum.name}")
      enum.values.foreach { value =>
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
      println(s" - ${union.name}")
      union.types.foreach { t =>
        println(s"   - ${t}")
      }
    }
  }

}
