package bitcoin.ws

import akka.stream.scaladsl._
import bitcoin.ws.FlowGraphs._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import support.AkkaStreamsTest

class FlowGraphsSpec extends FlatSpec with AkkaStreamsTest with Matchers with ScalaFutures {

//  implicit override def patienceConfig =
//      PatienceConfig(timeout = Span(1, Seconds), interval = Span(50, Millis))

  "flow" should "generate random prices" in {

    val source = price.random(100.0).take(40)

    val future = source.runWith(Sink.foreach(println))

    whenReady(future) { unit =>

    }
  }

}
