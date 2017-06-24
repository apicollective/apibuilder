package controllers

import io.apibuilder.apidoc.api.v0.models.{AttributeForm, User}
import lib.{Pagination, PaginatedCollection}
import models.MainTemplate
import scala.concurrent.Future
import java.util.UUID
import javax.inject.Inject
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.data.Forms._
import play.api.data._
import play.api.mvc.{Action, Controller}

class AttributesController @Inject() (val messagesApi: MessagesApi) extends Controller with I18nSupport {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def index(page: Int = 0) = Anonymous.async { implicit request =>
    for {
      attributes <- request.api.attributes.get(
        limit = Pagination.DefaultLimit+1,
        offset = page * Pagination.DefaultLimit
      )
    } yield {
      Ok(views.html.attributes.index(
        request.mainTemplate().copy(title = Some("Attributes")),
        attributes = PaginatedCollection(page, attributes)
      ))
    }
  }

  def show(name: String, page: Int) = Anonymous.async { implicit request =>
    for {
      attribute <- lib.ApiClient.callWith404(request.api.attributes.getByName(name))
      generators <- request.api.generatorWithServices.get(
        attributeName = Some(name),
        limit = Pagination.DefaultLimit+1,
        offset = page * Pagination.DefaultLimit
      )
    } yield {
      attribute match {
        case None => Redirect(routes.AttributesController.index()).flashing("warning" -> s"Attribute not found")
        case Some(attr) => {
          Ok(views.html.attributes.show(
            request.mainTemplate().copy(title = Some(attr.name)),
            attr,
            generatorWithServices = PaginatedCollection(page, generators)
          ))
        }
      }
    }
  }

  def create() = Authenticated { implicit request =>
    val filledForm = AttributesController.attributesFormData.fill(
      AttributesController.AttributeFormData(
        name = "",
        description = ""
      )
    )

    Ok(views.html.attributes.create(request.mainTemplate(), filledForm))
  }

  def createPost = Authenticated.async { implicit request =>
    val tpl = request.mainTemplate(Some("Add Attribute"))

    val form = AttributesController.attributesFormData.bindFromRequest
    form.fold (

      errors => Future {
        Ok(views.html.attributes.create(request.mainTemplate(), errors))
      },

      valid => {
        request.api.attributes.post(
          AttributeForm(
            name = valid.name,
            description = Some(valid.description.trim)
          )
        ).map { attribute =>
          Redirect(routes.AttributesController.index()).flashing("success" -> "Attribute created")
        }.recover {
          case r: io.apibuilder.apidoc.api.v0.errors.ErrorsResponse => {
            Ok(views.html.attributes.create(request.mainTemplate(), form, r.errors.map(_.message)))
          }
        }
      }

    )
  }

  def deletePost(name: String) = Authenticated.async { implicit request =>
    lib.ApiClient.callWith404(request.api.attributes.deleteByName(name)).map {
      case None => Redirect(routes.AttributesController.index()).flashing("warning" -> s"Attribute not found")
      case Some(_) => Redirect(routes.AttributesController.index()).flashing("success" -> s"Attribute deleted")
    }
  }
}

object AttributesController {

  case class AttributeFormData(
    name: String,
    description: String
  )

  private[controllers] val attributesFormData = Form(
    mapping(
      "name" -> nonEmptyText,
      "description" -> nonEmptyText
    )(AttributeFormData.apply)(AttributeFormData.unapply)
  )

}
