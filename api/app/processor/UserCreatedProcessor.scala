package processor

import cats.implicits._
import cats.data.ValidatedNec
import db._
import io.apibuilder.common.v0.models.MembershipRole
import io.apibuilder.task.v0.models.TaskType

import java.util.UUID
import javax.inject.Inject


class UserCreatedProcessor @Inject()(
                                      args: TaskProcessorArgs,
                                      usersDao: InternalUsersDao,
                                      organizationsDao: InternalOrganizationsDao,
                                      membershipRequestsDao: MembershipRequestsDao,
                                      emailVerificationsDao: InternalEmailVerificationsDao,
) extends TaskProcessorWithGuid(args, TaskType.UserCreated) {

  override def processRecord(userGuid: UUID): ValidatedNec[String, Unit] = {
    usersDao.findByGuid(userGuid).foreach { user =>
      organizationsDao.findAllByEmailDomain(user.email).foreach { org =>
        membershipRequestsDao.upsert(user, org, user, MembershipRole.Member)
      }
      emailVerificationsDao.upsert(user, user, user.email)
    }
    ().validNec
  }

}