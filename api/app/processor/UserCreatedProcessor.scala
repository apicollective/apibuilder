package processor

import cats.data.ValidatedNec
import db.UsersDao
import io.apibuilder.task.v0.models.TaskType

import javax.inject.Inject


class UserCreatedProcessor @Inject()(
  args: TaskProcessorArgs,
  usersDao: UsersDao
) extends TaskProcessorWithGuid(args, TaskType.UserCreated) {

  override def processRecord(userGuid: UUID): ValidatedNec[String, Unit] = {
    usersDao.processUserCreated(userGuid)
  }

}