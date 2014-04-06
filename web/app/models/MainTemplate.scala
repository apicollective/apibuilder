package models

import core.{ Organization, Resource, ServiceDescription, User }

case class MainTemplate(title: String,
                        org: Option[Organization] = None,
                        service: Option[ServiceDescription] = None,
                        version: Option[String] = None,
                        user: Option[User] = None,
                        resource: Option[Resource] = None) {

  // TODO
  def allServiceVersions: Seq[String] = Seq.empty

}
