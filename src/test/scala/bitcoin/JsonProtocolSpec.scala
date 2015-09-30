package bitcoin

import java.time.Instant

import bitcoin.JsonProtocol._

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import support.AkkaStreamsScalatest
import spray.json._

class JsonProtocolSpec extends FlatSpec with AkkaStreamsScalatest with Matchers with ScalaFutures {

  "format" should "serialize OHLCV" in {

    val trade = Trade(Instant.now(), 100, 200)
    val ohlcv = OHLCV(trade)

    val json = ohlcv.toJson

    println(s"json:\n$json")

  }

}
