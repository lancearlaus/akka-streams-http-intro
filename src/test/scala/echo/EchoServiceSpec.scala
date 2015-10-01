package echo

import org.scalatest.{Matchers, WordSpec}
import support.HttpServiceTest

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

}
