package generator

import com.gilt.apidocspec.models.Generator


case class CodeGenTarget(metaData: Generator, status: Status, codeGenerator: Option[CodeGenerator])

