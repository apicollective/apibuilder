package lib

import play.api.{ApplicationLoader, Environment, Mode, Play}

trait TestApplication {

  private[this] val env = Environment(new java.io.File("."), this.getClass.getClassLoader, Mode.Test)
  private[this] val context = ApplicationLoader.createContext(env)
  private[this] val app = ApplicationLoader(context).load(context)
  Play.start(app)

}
