package stream.rate

import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._

class PoissonSpec extends WordSpec with Matchers {

  "Iterator" should {

    "generate delay times" in {
      val mean = 40.millis
      val count = 100000
      val sum = Poisson.arrivalTimes(1 / mean).take(count).map(_.toMillis).sum
      val average = sum / count.toDouble

      println(s"average: $average")
      average should equal (mean.toMillis.toDouble +- 1)
    }

  }

}
