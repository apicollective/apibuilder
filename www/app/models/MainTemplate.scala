package models

import core.{ Model, Resource, ServiceDescription }
import apidoc.models.{ Organization, Service, User }

case class MainTemplate(title: String,
                        org: Option[Organization] = None,
                        service: Option[Service] = None,
                        version: Option[String] = None,
                        serviceDescription: Option[ServiceDescription] = None,
                        allServiceVersions: Seq[String] = Seq.empty,
                        user: Option[User] = None,
                        resource: Option[Resource] = None,
                        model: Option[Model] = None)
