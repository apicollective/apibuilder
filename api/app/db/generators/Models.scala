package db.generators

import com.gilt.apidoc.api.v0.models.ReferenceGuid
import org.joda.time.DateTime
import java.util.UUID

case class Service(
  guid: UUID,
  uri: String
)

case class ServiceForm(
  uri: String
)

case class Refresh(
  guid: UUID,
  service: ReferenceGuid,
  checkedAt: DateTime
)

case class Generator(
  guid: UUID,
  service: Service,
  key: String,
  name: String,
  language: Option[String] = None,
  description: Option[String] = None
)

case class GeneratorForm(
  key: String,
  name: String,
  language: Option[String] = None,
  description: Option[String] = None
)
