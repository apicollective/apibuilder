package controllers

import io.apibuilder.api.v0.models.AttributeValueForm
import models.{SettingSection, SettingsMenu}
import play.api.data.Forms._
import play.api.data._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OrganizationAttributesController @Inject() (
                                                   val apiBuilderControllerComponents: ApiBuilderControllerComponents
) extends ApiBuilderController {

  private[this] implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def redirect = Action { implicit request =>
    Redirect(routes.AttributesController.index())
  }

  def index(orgKey: String) = IdentifiedOrg.async { implicit request =>
    // TODO: Paginate once we exceed limit
    for {
      attributes <- request.api.attributes.get(
        limit = 100
      )
      attributeValues <- request.api.organizations.getAttributesByKey(
        orgKey,
        limit = 100
      )
    } yield {
      Ok(
        views.html.organization_attributes.index(
          request.mainTemplate().copy(
            title = Some(s"$orgKey: Attributes"),
            settings = Some(SettingsMenu(Some(SettingSection.Attributes)))
          ),
          values = attributeValues,
          otherAttributes = attributes.filter { attr =>
            attributeValues.find(_.attribute.name == attr.name).isEmpty
          }
        )
      )
    }
  }

  def edit(orgKey: String, name: String) = IdentifiedOrg.async { implicit request =>
    for {
      attr <- request.api.attributes.getByName(name)
      values <- request.api.organizations.getAttributesByKey(
        orgKey,
        name = Some(name),
        limit = 1
      )
    } yield {
      val filledForm = OrganizationAttributesController.formData.fill(
        OrganizationAttributesController.FormData(
          value = values.headOption.map(_.value)
        )
      )

      Ok(views.html.organization_attributes.edit(request.mainTemplate(), attr, filledForm))
    }
  }

  def editPost(orgKey: String, name: String) = IdentifiedOrg.async { implicit request =>
    request.api.attributes.getByName(name).flatMap { attr =>
      val tpl = request.mainTemplate(Some("Edit Attribute"))

      val form = OrganizationAttributesController.formData.bindFromRequest()
      form.fold (

        errors => Future {
          Ok(views.html.organization_attributes.edit(request.mainTemplate(), attr, errors))
        },

        valid => {
          valid.value.map(_.trim).getOrElse("") match {
            case "" => {
              // Delete existing attribute value if set
              request.api.organizations.deleteAttributesByKeyAndName(orgKey, name).map { value =>
                Redirect(routes.OrganizationAttributesController.index(orgKey)).flashing("success" -> s"Deleted value for $name")
              }.recover {
                case io.apibuilder.api.v0.errors.UnitResponse(404) => {
                  Redirect(routes.OrganizationAttributesController.index(orgKey)).flashing("success" -> s"Deleted value for $name")
                }
              }
            }
            case v => {
              request.api.organizations.putAttributesByKeyAndName(
                orgKey,
                attr.name,
                AttributeValueForm(value = v)
              ).map { value =>
                Redirect(routes.OrganizationAttributesController.index(orgKey)).flashing("success" -> s"Updated value for $name")
              }.recover {
                case r: io.apibuilder.api.v0.errors.ErrorsResponse => {
                  Ok(views.html.organization_attributes.edit(request.mainTemplate(), attr, form, r.errors.map(_.message)))
                }
              }
            }
          }
        }
      )
    }
  }
  
}

object OrganizationAttributesController {

  case class FormData(
    value: Option[String]
  )

  private[controllers] val formData = Form(
    mapping(
      "value" -> optional(text)
    )(FormData.apply)(FormData.unapply)
  )

}
