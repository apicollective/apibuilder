package models

import db.{ Organization, User }

case class MainTemplate(title: String,
                        org: Option[Organization] = None,
                        user: Option[User] = None,
                        service: Option[Service] = None,
                        resource: Option[Resource] = None) {

  lazy val organization: Option[Organization] = {
    if (org.isEmpty) {
      service.map(_.org)
    } else {
      org
    }
  }

}
