package actors

import helpers.{AsyncHelpers, DbHelpers, DefaultAppSpec, ExportHelpers}
import util.DbAuthorization

class TaskActorSpec extends DefaultAppSpec with AsyncHelpers with ExportHelpers with DbHelpers {

  "processes export" in {
    val exp = createDefaultExport()
    eventuallyInNSeconds(10) {
      internalExportsDao.findById(DbAuthorization.All, exp.id).value.db.processedAt.value
    }
  }

}
