package modules

import modules.clients._
import play.api.{Configuration, Environment, Mode}
import play.api.inject.Module

class TestClientModule extends Module {

  def bindings(env: Environment, conf: Configuration) = {
    assert(env.mode == Mode.Test, s"Mode expected to be '${Mode.Test}' and not '${env.mode}' for class[${getClass.getName}]")
    Seq(
      bind[GeneratorClientFactory].to[TestGeneratorClientFactory]
    )
  }
}