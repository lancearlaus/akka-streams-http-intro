package ohlc

import java.io.File

import akka.stream.io.SynchronousFileSource
import akka.stream.scaladsl._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FlatSpec, Matchers}
import streams.AkkaStreamsTest

class FlowsSpec extends FlatSpec with AkkaStreamsTest with Matchers with ScalaFutures {

  import ohlc.Flows._
  import streams.Flows._

  implicit override def patienceConfig =
      PatienceConfig(timeout = Span(2, Seconds), interval = Span(50, Millis))

  "flow" should "calculate OHLCV" in {
    val inSource = SynchronousFileSource(new File(getClass.getResource("/bitstampUSD.csv").getFile))

    val printSink = Sink.foreach[Any](println)

//    println(s"inputCsv: $inputCsv")

    val future = inSource.via(parse).via(trade).via(ohlcv(Daily)).runWith(printSink)

    whenReady(future) { done =>

//      println(s"output: ${builder.result.utf8String}")

    }
  }

}
