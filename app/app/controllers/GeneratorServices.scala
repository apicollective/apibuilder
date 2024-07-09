package controllers

import lib.{ApiClientProvider, PaginatedCollection, Pagination}
import play.api.mvc.{Action, AnyContent}

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class GeneratorServices @Inject() (
                                    val apiBuilderControllerComponents: ApiBuilderControllerComponents,
                                    apiClientProvider: ApiClientProvider
) extends ApiBuilderController {

  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def show(guid: UUID, page: Int = 0): Action[AnyContent] = Anonymous.async { implicit request =>
    for {
      serviceOption <- apiClientProvider.callWith404(request.api.generatorServices.getByGuid(guid))
      generators <- request.api.generatorWithServices.get(
        serviceGuid = Some(guid),
        limit = Pagination.DefaultLimit+1,
        offset = page * Pagination.DefaultLimit
      )
    } yield {
      serviceOption match {
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

  def deletePost(guid: UUID): Action[AnyContent] = Anonymous.async { implicit request =>
    for {
      _ <- request.api.generatorServices.deleteByGuid(guid)
    } yield {
      Redirect(routes.Generators.index()).flashing("success" -> "Generator service deleted")
    }
  }

}
