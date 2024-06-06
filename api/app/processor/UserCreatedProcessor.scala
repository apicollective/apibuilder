package processor

import cats.implicits._
import cats.data.ValidatedNec
import db._
import io.apibuilder.task.v0.models.TaskType
import lib.Role

import java.util.UUID
import javax.inject.Inject


class UserCreatedProcessor @Inject()(
  args: TaskProcessorArgs,
  usersDao: UsersDao,
  organizationsDao: OrganizationsDao,
  membershipRequestsDao: MembershipRequestsDao,
  emailVerificationsDao: EmailVerificationsDao,
) extends TaskProcessorWithGuid(args, TaskType.UserCreated) {

  override def processRecord(userGuid: UUID): ValidatedNec[String, Unit] = {
    usersDao.findByGuid(userGuid).foreach { user =>
      organizationsDao.findAllByEmailDomain(user.email).foreach { org =>
        membershipRequestsDao.upsert(user, org, user, Role.Member)
      }
      emailVerificationsDao.create(user, user, user.email)
    }
    ().validNec
  }

}