package util

import play.api.{ApplicationLoader, Environment, Mode, Play}

trait TestApplication extends Daos {

  private[this] val env = Environment(new java.io.File("."), this.getClass.getClassLoader, Mode.Test)
  private[this] val context = ApplicationLoader.createContext(env)
  override def app = ApplicationLoader(context).load(context)
  Play.start(app)

}
