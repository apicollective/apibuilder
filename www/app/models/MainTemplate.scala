package models

import core.{ Model, ServiceDescription }
import client.Apidoc.{ Organization, Service, User }

case class MainTemplate(title: String,
                        org: Option[Organization] = None,
                        service: Option[Service] = None,
                        version: Option[String] = None,
                        serviceDescription: Option[ServiceDescription] = None,
                        allServiceVersions: Seq[String] = Seq.empty,
                        user: Option[User] = None,
                        model: Option[Model] = None)
