package trades

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes.NotFound
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import trades.FlowGraphs._

import scala.concurrent.ExecutionContextExecutor

trait Service {

  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit def materializer: Materializer

  val logger: LoggingAdapter

  lazy val client: TradeHistoryClient = BitcoinChartsTradesClient()

  // Map to text/plain instead of csv for easy display in browser
  val contentType = ContentTypes.`text/plain`

  def fetch(symbol: String, transformer: Flow[Trade, ByteString, Any]) = {
    client.history(symbol).map[ToResponseMarshallable] {
      case Some(source) => HttpEntity.Chunked.fromData(contentType, source.via(transformer).via(chunk.min(2048)))
      case None => NotFound -> s"Invalid symbol or no data found for '$symbol'"
    }
  }

  def fetchRaw(symbol: String) = {
    client.rawHistory(symbol).map[ToResponseMarshallable] {
      case Some(source) => HttpEntity.Chunked.fromData(contentType, source.via(chunk.min(2048)))
      case None => NotFound -> s"Invalid symbol or no data found for '$symbol'"
    }
  }

  val tradeToCsv = Flow[Trade].via(trade.toRow()).via(csv.format)

  def periodicOHLCV(period: Period): Flow[Trade, ByteString, Unit] =
    ohlcv.periodic(period).via(ohlcv.intervalToRow).via(csv.format)

  val route =
    (get & pathPrefix("history")) {
      (path("trades" / Segment) & parameter('raw.as[Boolean] ? false)) { (symbol, raw) =>
        raw match {
          case true => complete(fetchRaw(symbol))
          case false => complete(fetch(symbol, tradeToCsv))
        }
      } ~
      pathPrefix("ohlcv") {
        path("daily" / Segment) { symbol =>
          complete(fetch(symbol, periodicOHLCV(Daily)))
        } ~
        path("hourly" / Segment) { symbol =>
          complete(fetch(symbol, periodicOHLCV(Hourly)))
        }
      }
    }

}
