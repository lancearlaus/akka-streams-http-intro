package bitcoin.ws

import org.scalatest.{Matchers, WordSpec}

class PoissonDelayIteratorSpec extends WordSpec with Matchers {

  "Iterator" should {

    "generate delay times" in {
      val mean = 40.0
      val count = 100000
      val sum = PoissonDelayIterator(mean).take(count).sum
      val average = sum / count.toDouble

      println(s"average: $average")
      average should equal (mean +- 0.1)


    }
  }


}
