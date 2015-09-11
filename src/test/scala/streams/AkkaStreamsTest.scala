package streams

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.{Suite, BeforeAndAfterAll}

import scala.concurrent.ExecutionContext

trait AkkaStreamsTest extends BeforeAndAfterAll { suite: Suite =>

  protected implicit var executionContext: ExecutionContext = _
  protected implicit var system: ActorSystem = _
  protected implicit var materializer: ActorMaterializer = _

  protected def systemName = "test-system"

  abstract override protected def beforeAll(): Unit = {
    super.beforeAll()
    system = ActorSystem(systemName)
    executionContext = system.dispatcher
    materializer = ActorMaterializer()
  }

  abstract override protected def afterAll(): Unit = {
    materializer.shutdown()
    executionContext = null
    system.shutdown()
    super.afterAll()
  }
}
