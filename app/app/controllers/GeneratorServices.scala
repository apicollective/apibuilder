package controllers

import lib.{Pagination, PaginatedCollection}
import java.util.UUID
import javax.inject.Inject
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.mvc.Controller

class GeneratorServices @Inject() (val messagesApi: MessagesApi) extends Controller with I18nSupport {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def show(guid: UUID, page: Int = 0) = Anonymous.async { implicit request =>
    for {
      service <- lib.ApiClient.callWith404(request.api.generatorServices.getByGuid(guid))
      generators <- request.api.generatorWithServices.get(
        serviceGuid = Some(guid),
        limit = Pagination.DefaultLimit+1,
        offset = page * Pagination.DefaultLimit
      )
    } yield {
      service match {
        case None => {
          Redirect(routes.Generators.index()).flashing("warning" -> s"Generator service not found")
        }
        case Some(service) => {
          Ok(views.html.generators.service(
            request.mainTemplate().copy(title = Some(service.uri)),
            service,
            generatorWithServices = PaginatedCollection(page, generators)
          ))
        }
      }
    }
  }

  def deletePost(guid: UUID) = Anonymous.async { implicit request =>
    for {
      result <- request.api.generatorServices.deleteByGuid(guid)
    } yield {
      Redirect(routes.Generators.index()).flashing("success" -> "Generator service deleted")
    }
  }

}
