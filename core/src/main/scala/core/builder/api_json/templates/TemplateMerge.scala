package builder.api_json.templates


import cats.data.ValidatedNec
import cats.implicits._
import io.apibuilder.api.json.v0.models.TemplateDeclaration

import scala.annotation.tailrec

private[templates] abstract class TemplateMerge[T](templates: Map[String, T]) {
  def applyTemplate(originalName: String, original: T, tplName: String, tpl: T): T

  def templateDeclarations(tpl: T): Seq[TemplateDeclaration]

  private[this] def format(value: String): String = value.toLowerCase().trim

  private[this] val templatesByName: Map[String, T] = templates.map { case (name, t) =>
    format(name) -> t
  }

  def allTemplates(templates: Option[Seq[TemplateDeclaration]]): ValidatedNec[String, Seq[TemplateDeclaration]] = {
    templates.getOrElse(Nil).map { tpl =>
      templatesByName.get(format(tpl.name)) match {
        case None => s"Cannot find template named '${tpl.name.trim}'".invalidNec
        case Some(o) => allTemplates(Some(templateDeclarations(o))).map { resolved =>
          Seq(tpl) ++ resolved
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
              applyTemplate(name, resource, one.name, tpl),
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
