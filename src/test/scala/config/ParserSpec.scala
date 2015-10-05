package config

import bitcoin.ws.Rate
import org.scalatest.{WordSpec, Matchers}

import scala.concurrent.duration._

class ParserSpec extends WordSpec with Matchers with CallParser {

  "Duration parser" should {

    "parse duration without length" in {
      parseAll(duration, "second").get shouldBe Duration(1, SECONDS)
    }

    "parse duration with length" in {
      parseAll(duration, "2 second").get shouldBe Duration(2, SECONDS)
      parseAll(duration, "2 seconds").get shouldBe Duration(2, SECONDS)
      parseAll(duration, "1/2 second").get shouldBe Duration(0.5, SECONDS)
      parseAll(duration, "1/(2*2) second").get shouldBe Duration(250, MILLISECONDS)
      parseAll(duration, "300 millis").get shouldBe Duration(300, MILLISECONDS)
    }
  }

  "Rate parser" should {

    "parse rate without duration length" in {
      parseAll(rate, "1 / second").get shouldBe Rate(1, Duration(1, SECONDS))
      parseAll(rate, "1 per second").get shouldBe Rate(1, Duration(1, SECONDS))
      parseAll(rate, "1 every second").get shouldBe Rate(1, Duration(1, SECONDS))
    }

    "parse rate with duration length" in {
      parseAll(rate, "2 per 2 seconds").get shouldBe Rate(2, Duration(2, SECONDS))
    }
  }

  "Arguments parser" should {

    "parse empty arguments" in {
      parseAll(args, "()").get shouldBe List.empty
    }

    "parse non-empty arguments" in {
      parseAll(args, "(300 per second)").get shouldBe List(Rate(300, Duration(1, SECONDS)))
      parseAll(args, "(1, 300 every 5 seconds, true)").get shouldBe List(1.0, Rate(300, Duration(5, SECONDS)), true)
    }
  }

  "Call parser" should {

    "parse call with empty arguments" in {
      parseAll(call, "name()").get shouldBe Call("name", List.empty)
    }

    "parse call with non-empty arguments" in {
      parseAll(call, "name(300 per second)").get shouldBe
        Call("name", List(Rate(300, Duration(1, SECONDS))))
      parseAll(call, "name(1, 300 every 5 seconds, true)").get shouldBe
        Call("name", List(1.0, Rate(300, Duration(5, SECONDS)), true))
      parseAll(call, "name(1, 300 every 5 seconds, true, 10 millis)").get shouldBe
        Call("name", List(1.0, Rate(300, Duration(5, SECONDS)), true, Duration(10, MILLISECONDS)))
    }
  }

}
