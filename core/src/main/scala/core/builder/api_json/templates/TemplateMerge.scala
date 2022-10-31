package builder.api_json.templates

import builder.api_json.InternalTemplateDeclarationForm

import scala.annotation.tailrec


private[templates] abstract class TemplateMerge[T](templates: Seq[T]) {
  def applyTemplate(original: T, tpl: T): T

  def label(tpl: T): String

  def templateDeclarations(tpl: T): Seq[InternalTemplateDeclarationForm]

  private[this] def format(value: String): String = value.toLowerCase().trim

  private[this] val templatesByName: Map[String, T] = templates.map { t =>
    format(label(t)) -> t
  }.toMap

  def allTemplates(templates: Seq[InternalTemplateDeclarationForm]): Seq[InternalTemplateDeclarationForm] = {
    templates.flatMap { tpl =>
      templatesByName.get(tpl.name.get) match {
        case None => Nil
        case Some(o) => Seq(tpl) ++ allTemplates(templateDeclarations(o))
      }
    }
  }

  @tailrec
  protected final def applyTemplates(resource: T, remaining: Seq[InternalTemplateDeclarationForm]): T = {
    remaining.toList match {
      case Nil => resource
      case one :: rest => {
        val tpl = templatesByName.getOrElse(one.name.get, sys.error(s"Cannot find template named '${one.name.get}'"))
        applyTemplates(
          applyTemplate(resource, tpl),
          rest
        )
      }
    }
  }

  @tailrec
  protected final def union(first: Seq[String], remaining: Seq[String]*): Seq[String] = {
    remaining.toList match {
      case Nil => first
      case one :: rest => union((first ++ one).distinct, rest: _*)
    }
  }
}
