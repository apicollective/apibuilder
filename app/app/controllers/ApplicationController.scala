package controllers

import lib.{Pagination, PaginatedCollection}
import javax.inject.Inject
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.mvc.{Action, Controller}

class ApplicationController @Inject() (
  val messagesApi: MessagesApi,
  apiClientProvider: ApiClientProvider
) extends Controller with I18nSupport {

  private[this] implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  def redirect() = Action { request =>
    Redirect(request.path + "/")
  }

  def redirectOrg(org: String) = Action { request =>
    Redirect(request.path + "/")
  }

  def index(orgsPage: Int = 0, membershipRequestsPage: Int = 0, publicOrgsPage: Int = 0) = Anonymous.async { implicit request =>
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
