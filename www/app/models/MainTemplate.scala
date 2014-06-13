package models

import core.{ Model, Resource, ServiceDescription }
import client.Apidoc.{ Service, User }
import apidoc.models.{ Organization }

case class MainTemplate(title: String,
                        org: Option[Organization] = None,
                        service: Option[Service] = None,
                        version: Option[String] = None,
                        serviceDescription: Option[ServiceDescription] = None,
                        allServiceVersions: Seq[String] = Seq.empty,
                        user: Option[User] = None,
                        resource: Option[Resource] = None,
                        model: Option[Model] = None)
