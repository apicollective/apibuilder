package lib

import play.api.ApplicationLoader.Context
import play.api.{ApplicationLoader, Environment, Mode, Play}

trait TestApplication {

  private val env = Environment(new java.io.File("."), this.getClass.getClassLoader, Mode.Test)
  private val context = Context.create(env)
  private val app = ApplicationLoader(context).load(context)
  Play.start(app)

}
