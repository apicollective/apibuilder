package services

import cats.data.ValidatedNec
import cats.implicits._
import db._
import io.apibuilder.api.v0.models.User
import io.apibuilder.common.v0.models.MembershipRole
import models.MembershipRequestsModel

import javax.inject.{Inject, Singleton}

@Singleton
class EmailVerificationsService @Inject()(
                                           emailVerificationConfirmationsDao: InternalEmailVerificationConfirmationsDao,
                                           membershipRequestsDao: MembershipRequestsDao,
                                           membershipRequestsModel: MembershipRequestsModel,
                                           organizationsDao: InternalOrganizationsDao
) {

  def confirm(user: Option[InternalUser], verification: InternalEmailVerification): ValidatedNec[String, Unit] = {
    validateExpiration(verification).map { _ =>
      val updatingUserGuid = user.map(_.guid).getOrElse(verification.userGuid)

      emailVerificationConfirmationsDao.upsert(updatingUserGuid, verification)
      organizationsDao.findAllByEmailDomain(verification.email).foreach { org =>
        membershipRequestsDao.findByOrganizationAndUserGuidAndRole(Authorization.All, org, verification.userGuid, MembershipRole.Member)
          .flatMap(membershipRequestsModel.toModel)
          .foreach { request =>
            membershipRequestsDao.acceptViaEmailVerification(updatingUserGuid, request, verification.email)
          }
      }
    }
  }

  private def validateExpiration(verification: InternalEmailVerification): ValidatedNec[String, Unit] = {
    if (verification.expiresAt.isBeforeNow) {
      s"Token for verificationGuid[${verification.guid}] is expired".invalidNec
    } else {
      ().validNec
    }
  }

}
