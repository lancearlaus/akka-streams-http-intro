package bitcoin.ws

import java.util.concurrent.TimeUnit

import akka.event.Logging
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import bitcoin.ws.FlowGraphs.trade
import bitcoin.ws.FlowGraphs.tick
import http.HttpService

import scala.concurrent.duration._


/**
 * Simple WebSocket service that emits random trades.
 * The timing of the trades follows a Poisson distribution.
 *
 * This class demonstrates the use of WebSockets, of course, as well as the use of a custom Actor-based Publisher.
 */
trait BitcoinTradesWebsocketService extends HttpService {

  private lazy val log = Logging(system, classOf[BitcoinTradesWebsocketService])

  protected def meanDelay = config.getDuration("service.bitcoin.trades.tick.meanDelay", TimeUnit.MILLISECONDS).millis

  abstract override def route =
    (get & pathPrefix("bitcoin")) {
        path("random"/"trades") {
          handleWebsocketMessages(randomTradesFlow)
        }
    } ~ super.route


  // A flow that periodically produces random trade messages while ignoring any incoming messages
  private lazy val randomTradesFlow: Flow[Message, Message, _] = Flow.wrap(
    Flow[Message].to(Sink.ignore),
    periodicRandomTradeSource.via(trade.toMessage)
  )(Keep.right)

  private def periodicRandomTradeSource =
    trade.periodic(trade.random(100.0), tick.Poisson(meanDelay))

}
