package trades

import java.time.Instant

import trades.JsonProtocol._

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import streams.AkkaStreamsTest
import spray.json._

class JsonProtocolSpec extends FlatSpec with AkkaStreamsTest with Matchers with ScalaFutures {

  "format" should "serialize OHLCV" in {

    val trade = Trade(Instant.now(), 100, 200)
    val ohlcv = OHLCV(trade)

    val json = ohlcv.toJson

    println(s"json:\n$json")

  }

}
