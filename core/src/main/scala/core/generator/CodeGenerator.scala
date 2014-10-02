package core.generator

import codegenerator.models.ServiceDescription

trait CodeGenerator {
  def generate(sd: ServiceDescription): String
}
