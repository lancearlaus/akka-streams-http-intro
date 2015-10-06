package stream.rate

import akka.stream.scaladsl.Source
import org.scalatest.{Matchers, WordSpec}
import support.AkkaStreamsTest

import scala.concurrent.Await
import scala.concurrent.duration._


class DelayStageSpec extends WordSpec with Matchers with AkkaStreamsTest {

  "Delay stage" should {

    "emit elements after constant delay" in {

      val delay = 250.millis
      val count = 10
      val future = Source(1 to count).via(constantDelay(delay)).runForeach(e => log.debug(e.toString))

      Await.result(future, count * delay * 2)

    }

    "emit elements at a constant rate" in {

      val rate = 1 every 250.millis
      val count = 10
      val future = Source(1 to count).via(constantRate(rate)).runForeach(e => log.debug(e.toString))

      Await.result(future, count / rate * 2)

    }

    "emit limited elements at a constant rate" in {

      val rate = 1 every 250.millis
      val count = 10
      val future = Source(1 to count).via(constantRate(rate).take(count/2)).runForeach(e => log.debug(e.toString))

      Await.result(future, count / rate * 2)

    }

    "emit elements at a Poisson mean arrival rate" in {

      val mean = 1 every 250.millis
      val count = 10
      val future = Source(1 to count).via(poissonRate(mean)).runForeach(e => log.debug(e.toString))

      Await.result(future, count / mean * 2)

    }

  }

}
