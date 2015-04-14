package core

case class VersionMigration(
  internal: Boolean
) {

  def makeFieldsWithDefaultsRequired(): Boolean = internal

  def injectApidocVersion(): Boolean = internal

}
