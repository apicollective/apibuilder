package models

import db.InternalMembershipRequest
import io.apibuilder.api.v0.models.MembershipRequest

import javax.inject.Inject

class MembershipRequestsModel @Inject() () {
  def toModel(mr: InternalMembershipRequest): Option[MembershipRequest] = {
    toModels(Seq(mr)).headOption
  }

  def toModels(requests: Seq[InternalMembershipRequest]): Seq[MembershipRequest] = {
  }
}