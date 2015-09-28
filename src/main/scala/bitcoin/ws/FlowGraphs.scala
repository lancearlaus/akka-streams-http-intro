package bitcoin.ws

import akka.actor.Cancellable
import akka.http.scaladsl.model.ws.{TextMessage, Message}
import akka.stream.scaladsl.{FlowGraph, ZipWith, Flow, Source}
import bitcoin.Trade
import spray.json._
import akka.stream.scaladsl.FlowGraph.Implicits._

import scala.util.Random

object FlowGraphs {

  object price {

    // A Source of random prices
    def random(initial: Double, drift: Double = 0.001, magnitude: Double = 0.005) =
      Source.repeat(Unit).scan(initial) { case (price, _) =>
        (price + ((Random.nextDouble + drift - 0.5) * (price * magnitude))).abs
      }
  }

  object trade {
    import bitcoin.JsonProtocol._
    
    private def randomAmount(lower: Double, upper: Double) =
      lower + (Random.nextDouble * (upper - lower))

    // A Source of random trades
    def random(initial: Double, amount: (Double, Double) = (0.5, 50)) =
      price.random(initial)
        .map(price => Trade(price, (randomAmount _).tupled(amount)))

    def periodic(trades: Source[Trade, Unit], ticks: Source[Unit, Cancellable]): Source[Trade, Cancellable] =
      Source(
        trades,
        ticks,
        ZipWith((trade: Trade, _:Unit) => trade)
      )((_, c, _) => c) {
        implicit b => (trades, ticks, zip) =>
          import FlowGraph.Implicits._

          trades ~> zip.in0
          ticks  ~> zip.in1

          zip.out
      }

    lazy val toMessage: Flow[Trade, Message, Unit] = Flow[Trade]
      .map(trade => TextMessage(trade.toJson.toString))
  }
}
