package modules

import modules.clients._
import play.api.{Configuration, Environment, Mode}
import play.api.inject.Module

class ProductionClientModule extends Module {

  def bindings(env: Environment, conf: Configuration) = {
    assert(env.mode == Mode.Prod, s"Mode expected to be '${Mode.Prod}' and not '${env.mode}' for class[${getClass.getName}]")
    Seq(
      bind[GeneratorClientFactory].to[ProductionGeneratorClientFactory]
    )
  }
}