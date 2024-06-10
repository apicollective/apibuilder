package processor

import io.apibuilder.task.v0.models.TaskType

import javax.inject.Inject

class TaskActorCompanion @Inject() (
                                     indexApplication: IndexApplicationProcessor,
                                     diffVersion: DiffVersionProcessor,
                                     cleanupDeletions: CleanupDeletionsProcessor,
                                     scheduleMigrateVersions: ScheduleMigrateVersionsProcessor,
                                     migrateVersion: MigrateVersionProcessor,
                                     userCreated: UserCreatedProcessor,
                                     scheduleSyncGeneratorServices: ScheduleSyncGeneratorServicesProcessor,
                                     syncGeneratorService: SyncGeneratorServiceProcessor,
                                     email: EmailProcessor,
                                     purgeOldDeleted: PurgeDeletedProcessor,
                                     checkInvariants: CheckInvariantsProcessor,
) {
  private[processor] val all: Map[TaskType, BaseTaskProcessor] = {
    import TaskType._
    Map(
      CheckInvariants -> checkInvariants,
      IndexApplication -> indexApplication,
      CleanupDeletions -> cleanupDeletions,
      DiffVersion -> diffVersion,
      MigrateVersion -> migrateVersion,
      ScheduleMigrateVersions -> scheduleMigrateVersions,
      UserCreated -> userCreated,
      ScheduleSyncGeneratorServices -> scheduleSyncGeneratorServices,
      SyncGeneratorService -> syncGeneratorService,
      Email -> email,
      PurgeOldDeleted -> purgeOldDeleted,
    )
  }

  def process(typ: TaskType): Unit = {
    all.getOrElse(typ, {
      sys.error(s"Failed to find processor for task type '$typ'")
    }).process()
  }
}
