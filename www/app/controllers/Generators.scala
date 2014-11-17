package controllers

import com.gilt.apidoc.models.{GeneratorCreateForm, GeneratorUpdateForm, Generator, Visibility, User}
import models.MainTemplate
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._

import scala.concurrent.Future

object Generators extends Controller {

  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  def list() = Authenticated.async { implicit request =>
    request.api.Generators.get().recover {
      case ex: Exception => Seq.empty
    }.map { generators =>
      Ok(views.html.generators.index(
        MainTemplate(
          user = Some(request.user),
          generators = generators,
          title = s"Generators"
        )
      ))
    }
  }

  def getByKey(key: String) = Authenticated.async { implicit request =>
    for {
      generator <- request.api.Generators.getByKey(key)
    } yield {
      generator match {
        case None => Redirect(routes.Generators.list()).flashing("warning" -> s"Generator not found")
        case Some(g) => {
          Ok(
            views.html.generators.details(
              request.mainTemplate("Generator Details").copy(generators = Seq(g))
            )
          )
        }
      }
    }
  }

  def postUpdate(key: String) = Authenticated.async { implicit request =>
    val tpl = MainTemplate(
      user = Some(request.user),
      title = s"Add Generator"
    )
    val boundForm = generatorUpdateForm.bindFromRequest
    boundForm.fold (

      errors => Future {
        InternalServerError
      },

      valid => {
        request.api.Generators.putByKey(GeneratorUpdateForm(
          visibility = valid.visibility.map(Visibility(_)),
          enabled = valid.enabled
        ), key).map(_ => Ok)
      }
    )
  }

  def create() = Authenticated { implicit request =>
    Ok(views.html.generators.form(
      request.mainTemplate("Add Generator"),
      1,
      generatorCreateForm
    ))
  }

  def postCreate() = Authenticated.async { implicit request =>
    val tpl = request.mainTemplate("Add Generator")
    val boundForm = generatorCreateForm.bindFromRequest
    boundForm.fold (

      errors => Future {
        Ok(views.html.generators.form(tpl, 1, errors))
      },

      (valid: GeneratorCreateData) => {
        valid.details.headOption.map { _ =>
          val futures: List[Future[Generator]] = valid.details.filter(_.selected).map { detail =>
            request.api.Generators.post(GeneratorCreateForm(
              key = detail.key,
              uri = valid.uri,
              visibility = Visibility(detail.visibility)
            ))
          }
          Future.sequence(futures).map { d =>
            Redirect(routes.Generators.list()).flashing("success" -> s"Generator(s) added")
          }.recover {
            case response: com.gilt.apidoc.error.ErrorsResponse => {
              Ok(views.html.generators.form(tpl, 1, boundForm, response.errors.map(_.message)))
            }
          }
        }.getOrElse {
          val existingGenF = request.api.Generators.get()
          val newGenF = new com.gilt.apidocgenerator.Client(valid.uri).generators.get()
          (for {
            existingGenerators <- existingGenF
            newGenerators <- newGenF
          } yield {
            val existingKeys = existingGenerators.filter(_.uri == valid.uri).map(_.key).toSet
            val d = newGenerators.toList.map(gen => GeneratorDetails(gen.key, Visibility.Public.toString, !existingKeys.contains(gen.key)))
            Ok(views.html.generators.form(tpl, 2, generatorCreateForm.fill(valid.copy(details = d))))
          }).recover {
            case response: com.gilt.apidoc.error.ErrorsResponse => {
              Ok(views.html.generators.form(tpl, 1, boundForm, response.errors.map(_.message)))
            }
          }
        }
      }
    )
  }

  def postRemove(key: String) = Authenticated.async { implicit request =>
    for {
      response <- request.api.Generators.deleteByKey(key)
    } yield {
      Redirect(routes.Generators.list()).flashing("success" -> s"Generator removed")
    }
  }

  case class GeneratorDetails(key: String, visibility: String, selected: Boolean)

  case class GeneratorCreateData(uri: String, details: List[GeneratorDetails])
  private val generatorCreateForm = Form(
    mapping(
      "uri" -> nonEmptyText,
      "details" -> Forms.list(
        mapping(
          "key" -> nonEmptyText,
          "visibility" -> nonEmptyText,
          "selected" -> boolean
        )(GeneratorDetails.apply)(GeneratorDetails.unapply)
      )
    )(GeneratorCreateData.apply)(GeneratorCreateData.unapply)
  )

  case class GeneratorUpdateData(visibility: Option[String], enabled: Option[Boolean])
  private val generatorUpdateForm = Form(
    mapping(
      "visibility" -> optional(nonEmptyText),
      "enabled" -> optional(boolean)
    )(GeneratorUpdateData.apply)(GeneratorUpdateData.unapply)
  )
}
