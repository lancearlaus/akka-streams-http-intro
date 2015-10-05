package bitcoin.ws

import akka.actor.Cancellable
import akka.event.Logging
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import bitcoin.ws.FlowGraphs.{tick, trade}
import com.typesafe.config.Config
import config.{Call, CallParser}
import http.HttpService

import scala.concurrent.duration.FiniteDuration


/**
 * Simple WebSocket service that emits random trades.
 * The timing of the trades follows a Poisson distribution.
 *
 * This class demonstrates the use of WebSockets and the use of a custom Actor-based Publisher.
 */
trait BitcoinTradesWebsocketService extends HttpService {
  import BitcoinTradesWebsocketService._

  private lazy val log = Logging(system, classOf[BitcoinTradesWebsocketService])

  protected lazy val ticks = tickSource("service.bitcoin.trades.ticks")

  abstract override def route =
    (get & path("bitcoin"/"random"/"trades")) {
      handleWebsocketMessages(randomTradesFlow)
    } ~ super.route


  // A flow that periodically produces random trade messages while ignoring any incoming messages
  private lazy val randomTradesFlow: Flow[Message, Message, _] = Flow.wrap(
    Flow[Message].to(Sink.ignore),
    periodicRandomTradeSource.via(trade.toMessage)
  )(Keep.right)

  private def periodicRandomTradeSource =
    trade.periodic(trade.random(100.0), ticks)

}

object BitcoinTradesWebsocketService extends CallParser {

  /**
   * Creates a tick Source from configuration property.
   *
   * Two types of tick sources are currently supoorted.
   * Uniform - Source that emits ticks at regular intervals
   *   Ex: service.bitcoin.trades.ticks: "uniform(1 every 250 millis)"
   * Poisson - Source that emits ticks according a Poisson distribution with the given mean interval
   *   Ex: service.bitcoin.trades.ticks: "poisson(100 per second)"
   *
   * @param path configuration property path from which to read configuration
   * @return cancellable tick source
   */
  def tickSource(path: String)(implicit config: Config): Source[FiniteDuration, Cancellable] = {
    val value = config.getString(path)
    parseAll(call, value) match {
      case Success(Call("uniform", List(rate: Rate)), _) => tick.uniform(rate)
      case Success(Call("poisson", List(rate: Rate)), _) => tick.poisson(rate)
      case NoSuccess(msg, _) =>
        throw new Exception(s"failed to parse configuration value (msg: $msg)")
    }
  }

}