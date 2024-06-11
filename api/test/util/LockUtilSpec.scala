package io.flow.postgresql.play.db

import io.flow.postgresql.play.db.util.LibPostgresqlPlaySpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.IntegrationPatience

import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class LockUtilSpec extends LibPostgresqlPlaySpec with BeforeAndAfterAll with IntegrationPatience {

  private implicit val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))

  private val dbName = "default"
  private val schema = "public"
  private val tableName = "lock_table1"
  private val idField = "id"
  private val dataField = "data"

  private[this] def lockUtil: DefaultLockUtil = init[DefaultLockUtil]

  override protected def beforeAll(): Unit = {
    dropTable(dbName, tableName)
    executeQuery(dbName, s"create table $tableName($idField bigint primary key, $dataField text)")
  }

  def doWork(called: AtomicBoolean, sleepMs: Long = 100): Unit = {
    Thread.sleep(sleepMs)
    called.set(true)
  }

  def doWork(called: AtomicInteger, sleepMs: Long): Unit = {
    Thread.sleep(sleepMs)
    called.incrementAndGet(): Unit
  }

  "Row locks" should {

    "lock on integer id" in {
      val helper = new ExecuteHelper
      val id = System.currentTimeMillis()
      executeQuery(dbName, s"insert into $tableName ($idField) values ($id) ")

      Future(lockUtil.withLock(schema, tableName, idField, id) { _ => helper.execute("first") })
      Future(lockUtil.withLock(schema, tableName, idField, id) { _ => helper.execute("second") })

      helper.waitForDiff must be >= 1000
    }

    "lock on text column" in {
      val helper = new ExecuteHelper
      val id = System.currentTimeMillis()
      val data = createTestId()
      executeQuery(dbName, s"insert into $tableName ($idField, $dataField) values ($id, '$data') ")

      Future(lockUtil.withLock(schema, tableName, dataField, data) { _ => helper.execute("first") })
      Future(lockUtil.withLock(schema, tableName, dataField, data) { _ => helper.execute("second") })

      helper.waitForDiff must be >= 1000
    }

    "throw an error when id does not exist in lock table" in {
      val id = 0L
      Try {
        lockUtil.withLock(schema, tableName, idField, id) { _ => () }
      } match {
        case Success(_) => sys.error("Expected future to fail")
        case Failure(ex) =>
          ex.getMessage must be(
            s"Attempted to lock for $idField [$id], but could not find a corresponding entry in $schema.$tableName"
          )
      }
    }

  }

  "Try advisory locks" should {
    "one piece of work with lock" in {
      val id = 0L
      val called = new AtomicBoolean(false)
      await {
        Future(lockUtil.tryAcquireWithLock(id) { _ => called.set(true) })
      }
      called.get must be(true)
    }

    "one piece of work with lock with provided transaction" in {
      val id = createTestId()
      val called = new AtomicBoolean(false)
      await {
        Future {
          db.withTransaction { tx =>
            lockUtil.tryAcquireWithLock(tx, id) { _ => called.set(true) }
          }
        }
      }
      called.get must be(true)
    }

    "throw an error when tryAcquireWithLock is not in a transaction" in {
      val id = createTestId()
      Try {
        db.withConnection { c =>
          lockUtil.tryAcquireWithLock(c, id) { _ => () }
        }
      } match {
        case Success(_) => sys.error("Expected future to fail")
        case Failure(ex) =>
          ex.getMessage must be(s"requirement failed: Must be in a transaction")
      }
    }

    "two try but only one can acquire lock" in {
      val id = 1L
      val aCalled = new AtomicBoolean(false)
      val bCalled = new AtomicBoolean(false)

      val a = Future(lockUtil.tryAcquireWithLock(id) { _ => doWork(aCalled) })
      val b = Future(lockUtil.tryAcquireWithLock(id) { _ => doWork(bCalled) })

      await(a)
      await(b)
      aCalled.get must not be bCalled.get
    }

    "long values do not collide" in {
      val id1 = Int.MaxValue.toLong + 1L
      val id2 = Int.MaxValue.toLong
      val aCalled = new AtomicBoolean(false)
      val bCalled = new AtomicBoolean(false)

      val a = Future(lockUtil.tryAcquireWithLock(id1) { _ => doWork(aCalled) })
      val b = Future(lockUtil.tryAcquireWithLock(id2) { _ => doWork(bCalled) })

      await(a)
      await(b)
      aCalled.get mustBe true
      bCalled.get mustBe true
    }

    "one piece of work with lock on string" in {
      val id = "id"
      val called = new AtomicBoolean(false)
      await {
        Future(lockUtil.tryAcquireWithLock(id) { _ => called.set(true) })
      }
      called.get must be(true)
    }

    "two try but only one can acquire lock on string" in {
      val id = "id"
      val aCalled = new AtomicBoolean(false)
      val bCalled = new AtomicBoolean(false)

      val a = Future(lockUtil.tryAcquireWithLock(id) { _ => doWork(aCalled) })
      val b = Future(lockUtil.tryAcquireWithLock(id) { _ => doWork(bCalled) })

      await(a)
      await(b)
      aCalled.get must not be bCalled.get
    }

    "try acquire lock concurrently" in {
      val id = "id"
      val counterLock = new AtomicInteger(0)
      val counterInside = new AtomicInteger(0)
      val counterOverall = new AtomicInteger(0)
      val size = 10000

      val futures = (1 to size).map(_ =>
        Future {
          counterInside.incrementAndGet()
          lockUtil.tryAcquireWithLock(id) { _ =>
            counterOverall.getAndIncrement()
            if (counterLock.getAndIncrement() != 0)
              fail()
            Thread.sleep(20)
            if (counterLock.getAndDecrement() != 1)
              fail()
          }
        }
      )

      await(Future.sequence(futures))

      counterLock.get() mustBe 0
      counterOverall.get() must be > 0
      counterOverall.get() must be < size
      counterInside.get() mustBe size
    }

    "try acquire lock concurrently on different ids" in {
      val id1 = "id1"
      val id2 = "id2"
      val counterLock1 = new AtomicInteger(0)
      val counterLock2 = new AtomicInteger(0)
      val counterInside1 = new AtomicInteger(0)
      val counterInside2 = new AtomicInteger(0)
      val counterOverall1 = new AtomicInteger(0)
      val counterOverall2 = new AtomicInteger(0)

      val size = 10000

      val futures1 = (1 to size).map(_ =>
        Future {
          counterInside1.incrementAndGet()
          lockUtil.tryAcquireWithLock(id1) { _ =>
            counterOverall1.getAndIncrement()
            if (counterLock1.getAndIncrement() != 0)
              fail()
            Thread.sleep(20)
            if (counterLock1.getAndDecrement() != 1)
              fail()
          }
        }
      )

      val futures2 = (1 to size).map(_ =>
        Future {
          counterInside2.incrementAndGet()
          lockUtil.tryAcquireWithLock(id2) { _ =>
            counterOverall2.getAndIncrement()
            if (counterLock2.getAndIncrement() != 0)
              fail()
            Thread.sleep(20)
            if (counterLock2.getAndDecrement() != 1)
              fail()
          }
        }
      )

      await(Future.sequence(futures1))
      await(Future.sequence(futures2))

      counterLock1.get() mustBe 0
      counterOverall1.get() must be > 0
      counterOverall1.get() must be < size
      counterInside1.get() mustBe size

      counterLock2.get() mustBe 0
      counterOverall2.get() must be > 0
      counterOverall2.get() must be < size
      counterInside2.get() mustBe size
    }
  }

  "Wait advisory locks" should {
    "one piece of work with lock" in {
      val called = new AtomicBoolean(false)
      await {
        Future(lockUtil.waitAcquireLock("id") { _ => called.set(true) })
      }
      called.get must be(true)
    }

    "one piece of work with lock with provided transaction" in {
      val id = createTestId()
      val called = new AtomicBoolean(false)
      await {
        Future {
          db.withTransaction { tx =>
            lockUtil.waitAcquireLock(tx, id) { _ => called.set(true) }
          }
        }
      }
      called.get must be(true)
    }

    "throw an error when not in a transaction" in {
      val id = createTestId()
      Try {
        db.withConnection { c => lockUtil.waitAcquireLock(c, id) { _ => () } }
      } match {
        case Success(_) => sys.error("Expected future to fail")
        case Failure(ex) =>
          ex.getMessage must be(s"requirement failed: Must be in a transaction")
      }
    }

    "only one at time acquire lock" in {
      val id = "id"
      val lock = new AtomicInteger(0)
      val counter = new AtomicInteger(0)
      val numThreads = 10

      val futures = (0 until numThreads).map { n =>
        Future {
          lockUtil.waitAcquireLock(id) { _ =>
            lock.getAndSet(n) mustBe 0: Unit

            doWork(counter, 50)

            lock.getAndSet(0) mustBe n: Unit
          }
        }
      }

      await(Future.sequence(futures))
      lock.get mustBe 0
      counter.get mustBe numThreads
    }

    "acquire lock concurrently" in {
      val id = "id"
      val counterLock = new AtomicInteger(0)
      val counterInside = new AtomicInteger(0)
      val counterOverall = new AtomicInteger(0)
      val size = 16

      val futures = (1 to size).map(_ =>
        Future {
          counterInside.incrementAndGet()
          lockUtil.waitAcquireLock(id) { _ =>
            counterOverall.getAndIncrement()
            if (counterLock.getAndIncrement() != 0)
              fail()
            Thread.sleep(5)
            if (counterLock.getAndDecrement() != 1)
              fail()
          }
        }
      )

      await(Future.sequence(futures))

      counterLock.get() mustBe 0
      counterOverall.get() mustBe size
      counterInside.get() mustBe size
    }

    "acquire lock concurrently on different ids" in {
      val id1 = "id1"
      val id2 = "id2"
      val counterLock1 = new AtomicInteger(0)
      val counterLock2 = new AtomicInteger(0)
      val counterInside1 = new AtomicInteger(0)
      val counterInside2 = new AtomicInteger(0)
      val counterOverall1 = new AtomicInteger(0)
      val counterOverall2 = new AtomicInteger(0)

      val size = 16

      val futures1 = (1 to size).map(_ =>
        Future {
          counterInside1.incrementAndGet()
          lockUtil.waitAcquireLock(id1) { _ =>
            counterOverall1.getAndIncrement()
            if (counterLock1.getAndIncrement() != 0)
              fail()
            Thread.sleep(5)
            if (counterLock1.getAndDecrement() != 1)
              fail()
          }
        }
      )

      val futures2 = (1 to size).map(_ =>
        Future {
          counterInside2.incrementAndGet()
          lockUtil.waitAcquireLock(id2) { _ =>
            counterOverall2.getAndIncrement()
            if (counterLock2.getAndIncrement() != 0)
              fail()
            Thread.sleep(5)
            if (counterLock2.getAndDecrement() != 1)
              fail()
          }
        }
      )

      await(Future.sequence(futures1))
      await(Future.sequence(futures2))

      counterLock1.get() mustBe 0
      counterOverall1.get() mustBe size
      counterInside1.get() mustBe size

      counterLock2.get() mustBe 0
      counterOverall2.get() mustBe size
      counterInside2.get() mustBe size
    }
  }

  "Permanent locks" should {
    "one piece of work with permanent lock for unique id" in {
      val id = createTestId()
      val called = new AtomicBoolean(false)
      await {
        Future(lockUtil.tryAcquireWithPermanentLock(id) { _ => called.set(true) })
      }
      called.get must be(true)
    }

    "two try but only one can acquire permanent lock for unique id" in {
      val id = createTestId()
      val aCalled = new AtomicBoolean(false)
      val bCalled = new AtomicBoolean(false)

      val a = Future(lockUtil.tryAcquireWithPermanentLock(id) { _ => doWork(aCalled) })
      val b = Future(lockUtil.tryAcquireWithPermanentLock(id) { _ => doWork(bCalled) })

      await(a)
      await(b)
      aCalled.get must not be bCalled.get
    }

  }

  class ExecuteHelper {
    var first: Option[Instant] = None
    var second: Option[Instant] = None

    def execute(which: String): Unit = {
      Thread.sleep(1000)
      which match {
        case "first" => first = Some(Instant.now)
        case "second" => second = Some(Instant.now)
      }
    }

    def waitForDiff: Int = eventually {
      val firstResult = first.get
      val secondResult = second.get

      // We use the absolute difference because we don't know which one will get the lock first
      Math.abs(secondResult.toEpochMilli - firstResult.toEpochMilli).toInt
    }
  }
}
