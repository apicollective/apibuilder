package controllers

import java.util.UUID
import javax.inject.Inject

class MembershipRequestReviews @Inject() (
  val apibuilderControllerComponents: ApibuilderControllerComponents
) extends ApibuilderController {

  private[this] implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  def accept(orgKey: String, membershipRequestGuid: UUID) = IdentifiedOrg.async { implicit request =>
    require(request.isAdmin, "You are not an administrator")

    for {
      _ <- request.api.MembershipRequests.postAcceptByGuid(membershipRequestGuid)
    } yield {
      Redirect(routes.Organizations.membershipRequests(orgKey)).flashing("success" -> "Request accepted")
    }
  }

  def decline(orgKey: String, membershipRequestGuid: UUID) = IdentifiedOrg.async { implicit request =>
    require(request.isAdmin, "You are not an administrator")

    for {
      review <- request.api.MembershipRequests.postDeclineByGuid(membershipRequestGuid)
    } yield {
      Redirect(routes.Organizations.membershipRequests(orgKey)).flashing("success" -> "Request declined")
    }
  }

}
