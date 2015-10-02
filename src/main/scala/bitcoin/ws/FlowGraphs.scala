package bitcoin.ws

import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.{PoisonPill, Cancellable}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{Flow, FlowGraph, Source, ZipWith}
import bitcoin.Trade
import spray.json._

import scala.concurrent.duration.FiniteDuration
import scala.util.Random
import scala.concurrent.duration._


object FlowGraphs {

  object price {

    // A Source of random prices
    def random(initial: Double, drift: Double = 0.001, magnitude: Double = 0.005) =
      Source.repeat(Unit).scan(initial) { case (price, _) =>
        (price + ((Random.nextDouble + drift - 0.5) * (price * magnitude))).abs
      }
  }

  object tick {

    /**
     * Emits elements with variable timing, according to a Poisson distribution with the given mean
     * delay between events. A Poisson distribution is often used to model arrival times of real-life
     * events.
     *
     * @param meanDelay mean delay between events, in milliseconds
     * @return cancellable Source that emits the current delay, after said delay
     */
    def Poisson(meanDelay: FiniteDuration): Source[FiniteDuration, Cancellable] = {
      val durations = PoissonDelayIterator(meanDelay.toMillis).map(_.toLong.millis)
      val actorSource = Source.actorPublisher[FiniteDuration](TickPublisher.props(durations))
      val cancellableSource: Source[FiniteDuration, Cancellable] =
        actorSource.mapMaterializedValue { actorRef =>
          new Cancellable {
            val cancelled = new AtomicBoolean()
            override def isCancelled: Boolean = cancelled.get
            override def cancel(): Boolean =
              if (cancelled.compareAndSet(false, true)) {
                actorRef ! PoisonPill
                true
              } else {
                false
              }
          }
        }
      cancellableSource
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

    def periodic(trades: Source[Trade, Unit], ticks: Source[Any, Cancellable]): Source[Trade, Cancellable] =
      Source(
        trades,
        ticks,
        ZipWith((trade: Trade, tick: Any) => trade.copy(timestamp = Instant.now))
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
