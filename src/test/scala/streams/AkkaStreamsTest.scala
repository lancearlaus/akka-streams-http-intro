package streams

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import org.scalatest.{BeforeAndAfterAll, Suite}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise, ExecutionContext}

trait AkkaStreamsTest extends BeforeAndAfterAll { suite: Suite =>

  protected implicit var executionContext: ExecutionContext = _
  protected implicit var system: ActorSystem = _
  protected implicit var materializer: ActorMaterializer = _

  protected var logger: LoggingAdapter = _

  protected def systemName = "test-system"

  abstract override protected def beforeAll(): Unit = {
    super.beforeAll()
    system = ActorSystem(systemName)
    executionContext = system.dispatcher
    materializer = ActorMaterializer()
    logger = Logging(system, getClass)
  }

  abstract override protected def afterAll(): Unit = {
    logger = null
    executionContext = null
    materializer.shutdown()
    system.shutdown()
    system.awaitTermination()
    super.afterAll()
  }
}
