package core.generator

trait CodeGenerator {
  def generate(sd: ScalaServiceDescription, userAgent: String): String
}
