package controllers

import play.api.mvc.{Action, AnyContent}

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class MembershipRequestReviews @Inject() (
  val apiBuilderControllerComponents: ApiBuilderControllerComponents
) extends ApiBuilderController {

  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def accept(orgKey: String, membershipRequestGuid: UUID): Action[AnyContent] = IdentifiedOrg.async { implicit request =>
    request.withAdmin {
      for {
        _ <- request.api.MembershipRequests.postAcceptByGuid(membershipRequestGuid)
      } yield {
        Redirect(routes.Organizations.membershipRequests(orgKey)).flashing("success" -> "Request accepted")
      }
    }
  }

  def decline(orgKey: String, membershipRequestGuid: UUID): Action[AnyContent] = IdentifiedOrg.async { implicit request =>
    request.withAdmin {
      for {
        _ <- request.api.MembershipRequests.postDeclineByGuid(membershipRequestGuid)
      } yield {
        Redirect(routes.Organizations.membershipRequests(orgKey)).flashing("success" -> "Request declined")
      }
    }
  }

}
