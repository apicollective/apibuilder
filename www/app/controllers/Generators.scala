package controllers

import com.bryzek.apidoc.api.v0.models.{GeneratorServiceForm, User}
import com.bryzek.apidoc.generator.v0.models.{Generator}
import lib.{Pagination, PaginatedCollection, Util}
import models.MainTemplate
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._

import scala.concurrent.Future

object Generators extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def redirect = Action { implicit request =>
    Redirect(routes.Generators.index())
  }

  def index(page: Int = 0) = Authenticated.async { implicit request =>
    for {
      generators <- request.api.comBryzekApidocGeneratorV0ModelsGenerators.getGenerators(
        limit = Pagination.DefaultLimit+1,
        offset = page * Pagination.DefaultLimit
      )
    } yield {
      Ok(views.html.generators.index(
        request.mainTemplate().copy(title = Some("Generators")),
        generators = PaginatedCollection(page, generators)
      ))
    }
  }

  def show(key: String) = Authenticated.async { implicit request =>
    for {
      generator <- lib.ApiClient.callWith404(request.api.comBryzekApidocGeneratorV0ModelsGenerators.getGeneratorsByKey(key))
      services <- request.api.generatorServices.get(generatorKey = Some(key))
    } yield {
      generator match {
        case None => Redirect(routes.Generators.index()).flashing("warning" -> s"Generator not found")
        case Some(g) => {
          Ok(views.html.generators.show(
            request.mainTemplate().copy(title = Some(g.name)),
            g,
            services.headOption
          ))
        }
      }
    }
  }

  def create() = Authenticated { implicit request =>
    val filledForm = generatorServiceCreateFormData.fill(
      GeneratorServiceCreateFormData(
        uri = ""
      )
    )

    Ok(views.html.generators.create(request.mainTemplate(), filledForm))
  }

  def createPost = Authenticated.async { implicit request =>
    val tpl = request.mainTemplate(Some("Add Generator"))

    val form = generatorServiceCreateFormData.bindFromRequest
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
          case r: com.bryzek.apidoc.api.v0.errors.ErrorsResponse => {
            Ok(views.html.generators.create(request.mainTemplate(), form, r.errors.map(_.message)))
          }
        }
      }

    )
  }

  case class GeneratorServiceCreateFormData(
    uri: String
  )

  private[this] val generatorServiceCreateFormData = Form(
    mapping(
      "uri" -> nonEmptyText
    )(GeneratorServiceCreateFormData.apply)(GeneratorServiceCreateFormData.unapply)
  )

}
