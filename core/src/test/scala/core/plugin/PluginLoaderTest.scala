package core.plugin

import java.util.Properties

import org.scalatest.{FunSpec, Matchers}

class PluginLoaderTest extends FunSpec with Matchers {
  it("Should load a single plugin") {
    val properties = new Properties()
    properties.setProperty("pluginKey", "core.plugin.SomePlugin")

    val loader = new PluginLoader {
      override lazy val pluginProperties = Seq(properties)
    }

    val plugins = loader.loadPlugins[TestPlugin]("pluginKey")
    plugins.size shouldBe 1
    plugins.foreach(_.ping shouldBe "ping")
  }

  it("Should load several plugins") {
    val properties1 = new Properties()
    properties1.setProperty("pluginKey", "core.plugin.SomePlugin, core.plugin.OtherPlugin")

    val properties2 = new Properties()
    properties2.setProperty("pluginKey", "core.plugin.SomePlugin")

    val loader = new PluginLoader {
      override lazy val pluginProperties = Seq(properties1, properties2)
    }

    val plugins = loader.loadPlugins[TestPlugin]("pluginKey")
    plugins.size shouldBe 3
    plugins.filter(_.ping == "ping").size shouldBe 2
    plugins.filter(_.ping == "pong").size shouldBe 1
  }

  it("Should load plugins with arguments") {
    val properties = new Properties()
    properties.setProperty("pluginKey", "core.plugin.PluginWithArgs")

    val loader = new PluginLoader {
      override lazy val pluginProperties = Seq(properties)
    }

    val plugins = loader.loadPlugins[TestPlugin]("pluginKey", "foo")
    plugins.size shouldBe 1
    plugins.foreach(_.ping shouldBe "foo")
  }

  it("Should not fail on bad class name") {
    val properties = new Properties()
    properties.setProperty("pluginKey", "core.plugin.SomePlugin, core.plugin.BadPlugin")

    val loader = new PluginLoader {
      override lazy val pluginProperties = Seq(properties)
    }

    val plugins = loader.loadPlugins[TestPlugin]("pluginKey")
    plugins.size shouldBe 1
    plugins.foreach(_.ping shouldBe "ping")
  }

  it("Should skip missing constructors") {
    val properties = new Properties()
    properties.setProperty("pluginKey", "core.plugin.SomePlugin, core.plugin.PluginWithArgs")

    val loader = new PluginLoader {
      override lazy val pluginProperties = Seq(properties)
    }

    val plugins1 = loader.loadPlugins[TestPlugin]("pluginKey")
    plugins1.size shouldBe 1
    plugins1.foreach(_.ping shouldBe "ping")

    val plugins2 = loader.loadPlugins[TestPlugin]("pluginKey", "pong")
    plugins2.size shouldBe 1
    plugins2.foreach(_.ping shouldBe "pong")
  }

  it("Should skip incompatible types") {
    val properties = new Properties()
    properties.setProperty("pluginKey", "core.plugin.SomePlugin, core.plugin.DifferentPlugin")

    val loader = new PluginLoader {
      override lazy val pluginProperties = Seq(properties)
    }

    val plugins1 = loader.loadPlugins[TestPlugin]("pluginKey")
    plugins1.size shouldBe 1
    plugins1.foreach(_.ping shouldBe "ping")

    val plugins2 = loader.loadPlugins[DifferentPlugin]("pluginKey")
    plugins2.size shouldBe 1
    plugins2.foreach(_.foo shouldBe "bar")
  }

  it("Should load from actual properties resource") {
    // see core/src/test/resources/apidoc/plugin.properties
    val plugins = PluginLoader.loadPlugins[TestPlugin]("pluginKey")
    plugins.size shouldBe 2
    plugins.filter(_.ping == "ping").size shouldBe 1
    plugins.filter(_.ping == "pong").size shouldBe 1
  }
}

trait TestPlugin {
  def ping: String
}

class SomePlugin extends TestPlugin {
  override val ping = "ping"
}

class OtherPlugin extends TestPlugin {
  override val ping = "pong"
}

class PluginWithArgs(override val ping: String) extends TestPlugin

class DifferentPlugin {
  val foo = "bar"
}
