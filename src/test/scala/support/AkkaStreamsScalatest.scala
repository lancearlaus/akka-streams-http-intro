package support

import akka.stream.ActorMaterializer
import org.scalatest.Suite

/**
 * Akka Streams testing support class that manages the lifecycle of a Materializer.
 */
trait AkkaStreamsScalatest extends AkkaScalatest { suite: Suite =>

  implicit var materializer: ActorMaterializer = _

  abstract override protected def beforeAll(): Unit = {
    super.beforeAll()
    materializer = ActorMaterializer()
  }

  abstract override protected def afterAll(): Unit = {
    materializer.shutdown()
    super.afterAll()
  }
}
