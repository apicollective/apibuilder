package core

import play.api.libs.json._

case class OrganizationMetadata(
  package_name: scala.Option[String] = None
)

object OrganizationMetadata {
  val Empty = OrganizationMetadata(package_name = None)

  implicit val organizationMetadataWrites = Json.writes[OrganizationMetadata]
}

