package generator

import com.gilt.apidocspec.models.Service

trait CodeGenerator {
  def generate(sd: Service): String
}
