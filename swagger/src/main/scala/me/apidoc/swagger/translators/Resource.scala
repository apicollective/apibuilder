package me.apidoc.swagger.translators

import com.gilt.apidoc.spec.v0.{ models => apidoc }
import com.wordnik.swagger.{ models => swagger }

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
      description = None,
      deprecation = None,
      operations = Seq(
        Option(path.getGet).map { Operation(resolver, apidoc.Method.Get, url, _) },
        Option(path.getPost).map { Operation(resolver, apidoc.Method.Post, url, _) },
        Option(path.getPut).map { Operation(resolver, apidoc.Method.Put, url, _) },
        Option(path.getDelete).map { Operation(resolver, apidoc.Method.Delete, url, _) },
        Option(path.getOptions).map { Operation(resolver, apidoc.Method.Options, url, _) },
        Option(path.getPatch).map { Operation(resolver, apidoc.Method.Patch, url, _) }
      ).flatten
    )
  }

}
