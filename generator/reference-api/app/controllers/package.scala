import java.util.UUID

import play.api.libs.json._
import play.api.libs.functional.syntax._

package object controllers {
  implicit val uuidReads = __.read[String].map(UUID.fromString)
  implicit val uuidWrites = {
    __.write[String].contramap { x: UUID => x.toString }
  }
}
