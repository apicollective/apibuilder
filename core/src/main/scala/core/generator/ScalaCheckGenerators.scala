package core.generator

import core._
import Text._

object ScalaCheckGenerators {
  def apply(json: String): String = {
    val sd = ServiceDescription(json)
    val ssd = new ScalaServiceDescription(sd)
    apply(ssd)
  }

  def apply(ssd: ScalaServiceDescription): String = {
    val defs = ssd.models.map { model =>
      val impl = arb(new ScalaDataType.ScalaModelType(model))
      s"implicit def arb${model.name}: org.scalacheck.Arbitrary[${model.name}] = $impl"
    }.mkString("\n\n")
    val packageName = ssd.name.toLowerCase
    s"""package $packageName.test.models {
  object Arbitrary {
    import $packageName.models._
${defs.indent(4)}
  }
}"""
  }

  def gen(f: ScalaField): String = s"${f.name} <- ${arb(f.datatype)}.arbitrary"

  def arb(d: ScalaDataType): String = {
    import ScalaDataType._
    d match {
      case x @ ScalaStringType => "org.scalacheck.Arbitrary(org.scalacheck.Gen.alphaStr)"
      case x @ ScalaIntegerType => "org.scalacheck.Arbitrary.arbInt"
      case x @ ScalaDoubleType => "org.scalacheck.Arbitrary.arbDouble"
      case x @ ScalaLongType => "org.scalacheck.Arbitrary.arbLong"
      case x @ ScalaBooleanType => "org.scalacheck.Arbitrary.arbBool"
      case x @ ScalaDecimalType => "org.scalacheck.Arbitrary.arbBigDecimal"
      case x @ ScalaObjectType => "org.scalacheck.Arbitrary.arbImmutableMap[String, String]"
      case x @ ScalaUnitType => "org.scalacheck.Arbitrary.arbUnit"
      case x @ ScalaUuidType => s"org.scalacheck.Arbitrary(org.scalacheck.Gen.resultOf { _: Unit => ${x.name}.randomUUID })"
      case x @ ScalaDateTimeIso8601Type => {
        s"""import org.scalacheck.Arbitrary
Arbitrary(Arbitrary.arbDate.arbitrary.map(d => new ${x.name}(d.getTime)))"""
      }
      case ScalaListType(inner) => {
        s"org.scalacheck.Arbitrary(org.scalacheck.Gen.listOf(${arb(inner)}.arbitrary))"
      }
      case ScalaOptionType(inner) => s"org.scalacheck.Arbitrary.arbOption(${arb(inner)})"
      case x: ScalaModelType => {
        val genFields: String = x.model.fields.map(gen).mkString("\n")
        val initFields = x.model.fields.map(f => s"${f.name} = ${f.name}").mkString(",\n")
        s"""org.scalacheck.Arbitrary {
  for {
${genFields.indent(4)}
  } yield {
    new ${x.model.name}(
${initFields.indent(6)}
    )
  }
}"""
      }
    }
  }
}
