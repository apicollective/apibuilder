package db.generators

import com.gilt.apidoc.api.v0.models.ReferenceGuid
import org.joda.time.DateTime
import java.util.UUID

case class Source(
  guid: UUID,
  uri: String
)

case class SourceForm(
  uri: String
)

case class Refresh(
  guid: UUID,
  source: ReferenceGuid,
  checkedAt: DateTime
)
