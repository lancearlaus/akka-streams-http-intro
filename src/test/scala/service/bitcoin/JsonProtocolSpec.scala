package service.bitcoin

import model.{Trade, OHLCV}
import service.bitcoin.JsonProtocol._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import spray.json._

class JsonProtocolSpec extends WordSpec with Matchers with ScalaFutures {

  "Protocol" should {

    "write Trade as JSON" in {
      val before = Trade(100, 200)
      val after = before.toJson.convertTo[Trade]

      after shouldBe before
    }

    "write OHLCV as JSON" in {
      val before = OHLCV(Trade(100, 200))
      val after = before.toJson.convertTo[OHLCV]

      after shouldBe before
    }

  }

}
