package service.bitcoin.ws

import akka.event.Logging
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Keep, Sink}
import service.bitcoin.ws.FlowGraphs.trade
import service.http.HttpService
import stream.rate.TickPublisher
import TickPublisher.config._


/**
 * Simple WebSocket service that emits random trades.
 * The timing of the trades follows a Poisson distribution by default (see reference.conf)
 *
 * This class demonstrates the use of WebSockets and the use of a custom Actor-based Publisher.
 */
trait BitcoinTradesWebsocketService extends HttpService {

  private lazy val log = Logging(system, classOf[BitcoinTradesWebsocketService])
  protected lazy val ticks = config.getTickSource("service.service.bitcoin.trades.ticks")

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
