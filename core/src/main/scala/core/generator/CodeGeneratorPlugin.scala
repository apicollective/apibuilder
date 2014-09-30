package core.generator

trait CodeGeneratorPlugin {
  def getTarget: CodeGenTarget
}
