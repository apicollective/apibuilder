package util

import io.flow.postgresql.Query
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.db.Database
import play.api.test.Helpers.{await, defaultAwaitTimeout}

import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.util.{Failure, Success, Try}

class LockUtilSpec extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterAll {

  private implicit val ec: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))

  private[this] def lockUtil: LockUtil = app.injector.instanceOf[LockUtil]
  private[this] def db: Database = app.injector.instanceOf[Database]

  private val tableName = "lock_table1"

  private[this] def exec(query: Query): Unit = {
    db.withConnection { c =>
      query.anormSql().executeUpdate()(c)
    }
  }

  override protected def beforeAll(): Unit = {
    exec(Query(s"drop table if exists $tableName"))
    exec(Query(s"create table $tableName(id bigint primary key, data text)"))
  }

  private[this] def doWork(called: AtomicInteger): Unit = {
    Thread.sleep(50)
    called.incrementAndGet(): Unit
  }

  "Wait advisory locks" should {
    "one piece of work with lock" in {
      val called = new AtomicBoolean(false)
      await {
        Future(lockUtil.lock("id") { _ => called.set(true) })
      }
      called.get must be(true)
    }

    "one piece of work with lock with provided transaction" in {
      val id = UUID.randomUUID.toString
      val called = new AtomicBoolean(false)
      await {
        Future {
          db.withTransaction { c =>
            lockUtil.lock(c)(id) { _ => called.set(true) }
          }
        }
      }
      called.get must be(true)
    }

    "throw an error when not in a transaction" in {
      val id = UUID.randomUUID.toString
      Try {
        db.withConnection { c => lockUtil.lock(c)(id) { _ => () } }
      } match {
        case Success(_) => sys.error("Expected future to fail")
        case Failure(ex) =>
          ex.getMessage must be(s"requirement failed: Must be in a transaction")
      }
    }

    "only one at time acquire lock" in {
      val id = "id"
      val counter = new AtomicInteger(0)
      val numThreads = 10
      var lockAcquired = 0

      val futures = (0 until numThreads).map { n =>
        Future {
          lockUtil.lock(id) { _ =>
            lockAcquired += 1
            doWork(counter)
          }
        }
      }

      await(Future.sequence(futures))
      counter.get mustBe lockAcquired
    }
  }

}
