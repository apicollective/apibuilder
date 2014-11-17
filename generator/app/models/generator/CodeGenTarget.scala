package generator

import com.gilt.apidocgenerator.models.Generator


case class CodeGenTarget(metaData: Generator, status: Status, codeGenerator: Option[CodeGenerator])

