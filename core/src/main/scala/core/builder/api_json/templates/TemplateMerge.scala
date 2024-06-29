package builder.api_json.templates


import cats.data.ValidatedNec
import cats.implicits._
import io.apibuilder.api.json.v0.models.TemplateDeclaration

import scala.annotation.tailrec

private[templates] abstract class TemplateMerge[T](templates: Map[String, T]) {
  def applyTemplate(original: T, tpl: T): T

  def templateDeclarations(tpl: T): Seq[TemplateDeclaration]

  private def format(value: String): String = value.toLowerCase().trim

  private val templatesByName: Map[String, T] = templates.map { case (name, t) =>
    format(name) -> t
  }

  def resolveTemplateDeclarations(templates: Option[Seq[TemplateDeclaration]]): ValidatedNec[String, Seq[TemplateDeclaration]] = {
    resolveTemplateDeclarations(templates.getOrElse(Nil).distinctBy(_.name), resolved = Nil)
  }

  private def resolveTemplateDeclarations(templates: Seq[TemplateDeclaration], resolved: Seq[String]): ValidatedNec[String, Seq[TemplateDeclaration]] = {
    templates.map { tpl =>
      val fName = format(tpl.name)
      if (resolved.contains(fName)) {
        s"Recursive template named '${tpl.name.trim}' found. Remove this template declaration as it results in an infinite loop".invalidNec
      } else {
        templatesByName.get(format(tpl.name)) match {
          case None => s"Cannot find template named '${tpl.name.trim}'".invalidNec
          case Some(o) => resolveTemplateDeclarations(templateDeclarations(o), resolved = resolved ++ Seq(fName)).map { newTemplates =>
            Seq(tpl) ++ newTemplates
          }
        }
      }
    }.sequence.map(_.flatten)
  }

  @tailrec
  protected final def applyTemplates(name: String, resource: T, remaining: Seq[TemplateDeclaration]): T = {
    remaining.toList match {
      case Nil => resource
      case one :: rest => {
        templatesByName.get(format(one.name)) match {
          case None => {
            // Template not found. Will be validated by ApiJsonServiceValidator
            applyTemplates(name, resource, rest)
          }
          case Some(tpl) => {
            applyTemplates(
              name,
              applyTemplate(resource, tpl),
              rest
            )
          }
        }
      }
    }
  }


  @tailrec
  protected final def union(first: Seq[String], remaining: Seq[String]*): Option[Seq[String]] = {
    remaining.toList match {
      case Nil if first.isEmpty => None
      case Nil => Some(first)
      case one :: rest => union((first ++ one).distinct, rest: _*)
    }
  }
}
