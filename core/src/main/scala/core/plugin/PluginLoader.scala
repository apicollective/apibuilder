package core.plugin

import java.io.InputStream
import java.net.URL
import java.util.Properties
import scala.reflect.runtime.universe._
import scala.util.control.NonFatal


object PluginLoader extends PluginLoader

trait PluginLoader {
  def loadPlugins[T](pluginKey: String, args: String*)(implicit tag: TypeTag[T]): Seq[T] = {

    def newInstance(className: String): Option[T] = try {
      if (tag.mirror.staticClass(className).toType <:< tag.tpe) {
        val clazz = Class.forName(className)
        val ctor = clazz.getConstructor(args.map(_.getClass): _*)
        val instance = ctor.newInstance(args: _*)
        Some(instance.asInstanceOf[T])
      } else {
        None
      }
    } catch {
      case NonFatal(e) =>
        None
    }

    for {
      property <- pluginProperties
      value = property.getProperty(pluginKey) if (value != null)
      className <- value.split(",")
      instance <- newInstance(className.trim)
    } yield (instance)
  }

  lazy val pluginProperties: Seq[Properties] = {
    import scala.collection.JavaConverters._
    this.getClass.getClassLoader.getResources("apidoc/plugin.properties").asScala.flatMap(readProperties).toSeq
  }

  private def readProperties(url: URL): Option[Properties] = {
    safeReadUrl(url) { is =>
      val props = new Properties()
      props.load(is)
      props
    }
  }

  private def safeReadUrl[T](url: URL)(f: InputStream => T): Option[T] = {
    try {
      val input = url.openStream()
      try {
        Some(f(input))
      } finally {
        input.close()
      }
    } catch {
      case e: Exception => None
    }
  }
}
