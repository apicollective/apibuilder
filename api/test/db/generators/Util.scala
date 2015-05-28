package db.generators

import java.util.UUID

object Util {

  def createGeneratorSource(
    form: SourceForm = createGeneratorSourceForm()
  ): Source = {
    SourcesDao.create(db.Util.createdBy, form)
  }

  def createGeneratorSourceForm(
    uri: String = s"http://test.generator.${UUID.randomUUID}"
  ): SourceForm = {
    SourceForm(
      uri = uri
    )
  }

  def createGeneratorRefresh(
    source: Source = createGeneratorSource()
  ): Refresh = {
    RefreshesDao.upsert(db.Util.createdBy, source)
    RefreshesDao.findAll(sourceGuid = Some(source.guid)).headOption.getOrElse {
      sys.error("Failed to create refresh")
    }
  }

}
