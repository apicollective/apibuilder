package controllers

import io.apibuilder.api.v0.models.GeneratorServiceForm
import lib.{ApiClientProvider, PaginatedCollection, Pagination}
import play.api.data.Forms.*
import play.api.data.*
import play.api.mvc.{Action, AnyContent}

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject

class Generators @Inject() (
                             val apiBuilderControllerComponents: ApiBuilderControllerComponents,
                             apiClientProvider: ApiClientProvider
) extends ApiBuilderController {

  private implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def redirect: Action[AnyContent] = Action { implicit request =>
    Redirect(routes.Generators.index())
  }

  def index(page: Int = 0): Action[AnyContent] = Anonymous.async { implicit request =>
    for {
      generators <- request.api.generatorWithServices.get(
        limit = Pagination.DefaultLimit+1,
        offset = page * Pagination.DefaultLimit
      )
    } yield {
      Ok(views.html.generators.index(
        request.mainTemplate().copy(title = Some("Generators")),
        generatorWithServices = PaginatedCollection(page, generators)
      ))
    }
  }

  def show(key: String): Action[AnyContent] = Anonymous.async { implicit request =>
    for {
      generator <- apiClientProvider.callWith404(request.api.generatorWithServices.getByKey(key))
    } yield {
      generator match {
        case None => Redirect(routes.Generators.index()).flashing("warning" -> s"Generator not found")
        case Some(gws) => {
          Ok(views.html.generators.show(
            request.mainTemplate().copy(title = Some(gws.generator.name)),
            gws
          ))
        }
      }
    }
  }

  def create(): Action[AnyContent] = Identified { implicit request =>
    val filledForm = Generators.generatorServiceCreateFormData.fill(
      Generators.GeneratorServiceCreateFormData(
        uri = ""
      )
    )

    Ok(views.html.generators.create(request.mainTemplate(), filledForm))
  }

  def createPost: Action[AnyContent] = Identified.async { implicit request =>
    val form = Generators.generatorServiceCreateFormData.bindFromRequest()
    form.fold (

      errors => Future {
        Ok(views.html.generators.create(request.mainTemplate(), errors))
      },

      valid => {
        request.api.generatorServices.post(
          GeneratorServiceForm(
            uri = valid.uri
          )
        ).map { _ =>
          Redirect(routes.Generators.index()).flashing("success" -> "Generator created")
        }.recover {
          case r: io.apibuilder.api.v0.errors.ErrorsResponse => {
            Ok(views.html.generators.create(request.mainTemplate(), form, r.errors.map(_.message)))
          }
        }
      }

    )
  }

}

object Generators {

  case class GeneratorServiceCreateFormData(
    uri: String
  )
  object GeneratorServiceCreateFormData {
    def unapply(d: GeneratorServiceCreateFormData): Option[String] = Some(d.uri)
  }

  private[controllers] val generatorServiceCreateFormData = Form(
    mapping(
      "uri" -> nonEmptyText
    )(GeneratorServiceCreateFormData.apply)(GeneratorServiceCreateFormData.unapply)
  )

}
