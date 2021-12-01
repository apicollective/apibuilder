package me.apidoc.swagger.translators

import me.apidoc.swagger.Util
import io.apibuilder.spec.v0.{ models => apidoc }
import io.swagger.{ models => swagger }

object Resource {

  def apply(
    resolver: Resolver,
    model: apidoc.Model,
    url: String,
    path: swagger.Path
  ): apidoc.Resource = {
    // getVendorExtensions
    // getParameters
    apidoc.Resource(
      `type` = model.name,
      plural = model.plural,
      path = None,
      description = None,
      deprecation = None,
      operations = Seq(
        Option(path.getGet).map { Operation(resolver, model.name, apidoc.Method.Get, url, _) },
        Option(path.getPost).map { Operation(resolver, model.name, apidoc.Method.Post, url, _) },
        Option(path.getPut).map { Operation(resolver, model.name, apidoc.Method.Put, url, _) },
        Option(path.getDelete).map { Operation(resolver, model.name, apidoc.Method.Delete, url, _) },
        Option(path.getOptions).map { Operation(resolver, model.name, apidoc.Method.Options, url, _) },
        Option(path.getPatch).map { Operation(resolver, model.name, apidoc.Method.Patch, url, _) }
      ).flatten
    )
  }

  def mergeAll(resources: Seq[apidoc.Resource]): Seq[apidoc.Resource] = {
    resources.groupBy(_.`type`).flatMap {
      case (_, resources) => {
        resources.toList match {
          case Nil => Nil
          case resource :: Nil => Seq(resource)
          case r1 :: r2 :: Nil => Seq(merge(r1, r2))
          case r1 :: r2 :: rest => mergeAll(Seq(merge(r1, r2)) ++ rest)
        }
      }
    }.toSeq
  }

  def merge(r1: apidoc.Resource, r2: apidoc.Resource): apidoc.Resource = {
    r1.copy(
      description = Util.choose(r1.description, r2.description),
      deprecation = Util.choose(r1.deprecation, r2.deprecation),
      operations = r1.operations ++ r2.operations
    )
  }

}
