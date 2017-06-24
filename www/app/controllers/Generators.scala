package controllers

import io.apibuilder.api.v0.models.{GeneratorServiceForm, User}
import io.apibuilder.generator.v0.models.Generator
import lib.{Pagination, PaginatedCollection}
import models.MainTemplate
import play.api.data.Forms._
import play.api.data._

import scala.concurrent.Future

import javax.inject.Inject
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.mvc.{Action, Controller}

class Generators @Inject() (val messagesApi: MessagesApi) extends Controller with I18nSupport {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def redirect = Action { implicit request =>
    Redirect(routes.Generators.index())
  }

  def index(page: Int = 0) = Anonymous.async { implicit request =>
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

  def show(key: String) = Anonymous.async { implicit request =>
    for {
      generator <- lib.ApiClient.callWith404(request.api.generatorWithServices.getByKey(key))
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

  def create() = Authenticated { implicit request =>
    val filledForm = Generators.generatorServiceCreateFormData.fill(
      Generators.GeneratorServiceCreateFormData(
        uri = ""
      )
    )

    Ok(views.html.generators.create(request.mainTemplate(), filledForm))
  }

  def createPost = Authenticated.async { implicit request =>
    val tpl = request.mainTemplate(Some("Add Generator"))

    val form = Generators.generatorServiceCreateFormData.bindFromRequest
    form.fold (

      errors => Future {
        Ok(views.html.generators.create(request.mainTemplate(), errors))
      },

      valid => {
        request.api.generatorServices.post(
          GeneratorServiceForm(
            uri = valid.uri
          )
        ).map { generator =>
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

  private[controllers] val generatorServiceCreateFormData = Form(
    mapping(
      "uri" -> nonEmptyText
    )(GeneratorServiceCreateFormData.apply)(GeneratorServiceCreateFormData.unapply)
  )

}
