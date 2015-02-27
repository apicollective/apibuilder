package me.apidoc.avro

import org.scalatest.{FunSpec, Matchers}

class ParserSpec extends FunSpec with Matchers {

  val dir = "avro/src/test/resources/me/apidoc/avro"

  it("parses") {
    //Parser().parse(s"$dir/mobile-tapstream.avpr")
    //Parser().parse(s"$dir/simple-protocol.avpr")

    //Parser().parse(s"$dir/gfc-avro.avdl")
    //Parser().parse(s"$dir/simple-protocol-with-gfc.avpr")
    Parser().parse(s"$dir/mobile-tapstream.avpr")
  }

}
