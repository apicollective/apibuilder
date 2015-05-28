package db.generators

import com.gilt.apidoc.api.v0.models.GeneratorService
import org.joda.time.DateTime
import java.util.UUID

case class Refresh(
  guid: UUID,
  service: GeneratorService,
  checkedAt: DateTime
)
