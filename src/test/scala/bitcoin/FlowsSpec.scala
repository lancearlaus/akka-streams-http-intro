package bitcoin

import java.io.File

import akka.stream.io.SynchronousFileSource
import akka.stream.scaladsl._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FlatSpec, Matchers}
import streams.AkkaStreamsTest

class FlowsSpec extends FlatSpec with AkkaStreamsTest with Matchers with ScalaFutures {

  import bitcoin.FlowGraphs._

  implicit override def patienceConfig =
      PatienceConfig(timeout = Span(1, Seconds), interval = Span(50, Millis))

  "flow" should "calculate OHLCV" in {
    val inFile = new File(getClass.getResource("/bitstampUSD.csv").getFile)
    val inSource = SynchronousFileSource(inFile)

    val printSink = Sink.foreach[Any](println)
    def collectSink[T] = Sink.fold[List[T], T](List.empty[T])(_ :+ _)

//    println(s"inputCsv: $inputCsv")

    val future = inSource.via(csv.parse())
      .via(trade.fromRow)
      .via(ohlcv.periodic(Daily))
      .runWith(collectSink)

    whenReady(future) { list =>

//      println(s"Result:\n")
//      list.foreach(println)

      val count = list.map({ case (_, ohlcv) => ohlcv.count }).sum

      count shouldBe 20000
//      println(s"output: ${builder.result.utf8String}")

    }
  }

}
