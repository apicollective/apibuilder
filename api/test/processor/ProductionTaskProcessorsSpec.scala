package processor

import io.apibuilder.task.v0.models.TaskType
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

final class ProductionTaskProcessorsSpec extends PlaySpec with GuiceOneAppPerSuite {

  private[this] def companion = app.injector.instanceOf[TaskActorCompanion]

  "each task type is assigned a processor" in {
    val missing = TaskType.all.filterNot(companion.all.contains)
    if (missing.nonEmpty) {
      sys.error(
        s"TaskActorCompanion: Missing processor for task type(s): ${missing.map(_.toString).mkString(", ")}",
      )
    }
  }

  "each task type is assigned to at most one processor" in {
    val dups = companion.all.values.toSeq.groupBy(_.typ).filter { case (_, v) => v.length > 1 }.map { case (t, all) =>
      s"Task type $t is assigned to more than 1 processor: " + all.map(_.getClass.getName).mkString(", ")
    }
    if (dups.nonEmpty) {
      sys.error(dups.mkString(", "))
    }
  }
}
