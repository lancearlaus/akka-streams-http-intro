package bitcoin.ws

import akka.event.Logging
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import bitcoin.ws.FlowGraphs.trade
import http.HttpService

import scala.concurrent.duration._

trait BitcoinTradesWebsocketService extends HttpService {

  private lazy val log = Logging(system, classOf[BitcoinTradesWebsocketService])

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

  private lazy val periodicRandomTradeSource =
    trade.periodic(trade.random(100.0), Source(0.seconds, 1.second, Unit))

}
