package core.generator

import com.gilt.apidocgenerator.models.ServiceDescription

trait CodeGenerator {
  def generate(sd: ServiceDescription): String
}
