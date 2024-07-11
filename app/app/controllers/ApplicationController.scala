package controllers

import lib.{ApiClientProvider, PaginatedCollection, Pagination}
import play.api.mvc.{Action, AnyContent}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ApplicationController @Inject() (
                                        val apiBuilderControllerComponents: ApiBuilderControllerComponents,
                                        apiClientProvider: ApiClientProvider
) extends ApiBuilderController {

  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def redirect(): Action[AnyContent] = Action { request =>
    Redirect(request.path + "/")
  }

  def redirectOrg(org: String): Action[AnyContent] = Action { request =>
    Redirect(request.path + "/")
  }

  def index(orgsPage: Int = 0, membershipRequestsPage: Int = 0, publicOrgsPage: Int = 0): Action[AnyContent] = Anonymous.async { implicit request =>
    request.user match {
      case None => {
        for {
          publicOrgs <- request.api.Organizations.get(
            limit = Pagination.DefaultLimit+1,
            offset = publicOrgsPage * Pagination.DefaultLimit
          )
        } yield {
          Ok(
            views.html.index(
              request.mainTemplate(title = Some("Organizations")),
              PaginatedCollection(orgsPage, Seq.empty),
              PaginatedCollection(membershipRequestsPage, Seq.empty),
              PaginatedCollection(publicOrgsPage, publicOrgs)
            )
          )
        }
      }

      case Some(user) => {
        for {
          orgs <- request.api.Organizations.get(
            userGuid = Some(user.guid),
            limit = Pagination.DefaultLimit+1,
            offset = orgsPage * Pagination.DefaultLimit
          )
          membershipRequests <- request.api.MembershipRequests.get(
            userGuid = Some(user.guid),
            limit = Pagination.DefaultLimit+1,
            offset = membershipRequestsPage * Pagination.DefaultLimit
          )
          publicOrgs <- request.api.Organizations.get(
            limit = Pagination.DefaultLimit+1,
            offset = publicOrgsPage * Pagination.DefaultLimit
          )
        } yield {
          Ok(
            views.html.index(
              request.mainTemplate(title = Some("Your Organizations")),
              PaginatedCollection(orgsPage, orgs),
              PaginatedCollection(membershipRequestsPage, membershipRequests),
              PaginatedCollection(publicOrgsPage, publicOrgs)
            )
          )
        }
      }
    }
  }

}
