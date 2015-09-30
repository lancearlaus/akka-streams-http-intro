package echo

import org.scalatest.{Matchers, WordSpec}
import support.HttpServiceScalatest

class EchoServiceSpec extends WordSpec
    with EchoService
    with Matchers
    with HttpServiceScalatest
{

  "Echo service" should {

    "echo message" in {
      val message = "test"
      Get(s"/echo/$message") ~> route ~> check {
        responseAs[String] shouldEqual message
      }
    }

  }

}
