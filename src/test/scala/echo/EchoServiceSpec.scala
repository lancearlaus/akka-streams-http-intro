package echo

import org.scalatest.{Matchers, WordSpec}
import support.HttpServiceTest

import scala.concurrent.duration.Duration

class EchoServiceSpec
    extends WordSpec
    with Matchers
    with EchoService
    with HttpServiceTest
{

  "Echo service" should {

    "echo message" in {
      val message = "test"
      Get(s"/echo/$message") ~> route ~> check {
        responseAs[String] shouldEqual message
      }
    }

  }

  "Duration" should {

    "not parse invalid time unit" in {
      val duration = Duration(1, "bogus")

      duration.unit should not be (null)
    }
  }

}
