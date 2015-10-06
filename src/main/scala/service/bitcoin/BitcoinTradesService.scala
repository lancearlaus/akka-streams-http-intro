package service.bitcoin

import akka.event.Logging
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString
import model.{Periodic, Trade}
import service.bitcoin.FlowGraphs._
import service.http.HttpService
import stream.csv._
import stream.http._

/**
 * Calculates Open-High-Low-Close-Volume aggregates of Bitcoin trades.
 *
 * This class demonstrates the use of flows in transforming a response entity from an underlying service and
 * the parsing of complex path segments.
 *
 */
trait BitcoinTradesService extends HttpService {

  private lazy val log = Logging(system, classOf[BitcoinTradesService])
  protected lazy val tradesClient: BitcoinTradesClient = BitcoinChartsTradesClient()

  abstract override def route =
    (get & pathPrefix("bitcoin")) {
      path("price" / PeriodicSegment / Segment / Segment) {
        (periodic, exchange, currency) =>
          val symbol = exchange + currency
          complete(fetch(symbol, periodicOHLCV(periodic)))
      } ~
      (path("trades" / Segment / Segment) & parameter('raw.as[Boolean] ? false)) {
        (exchange, currency, raw) =>
          val symbol = exchange + currency
          raw match {
            case true => complete(fetchRaw(symbol))
            case false => complete(fetch(symbol, tradeToCsv))
          }
      }
    } ~ super.route

  // Parse a path segment as a Periodic
  val PeriodicSegment = Segment.tflatMap {
    case Tuple1(s) => Periodic.unapply(s).map(p => Tuple1(p))
    case _ => None
  }

  private val tradeToCsv = Flow[Trade].via(trade.toRow()).via(formatRow)

  private def periodicOHLCV(periodic: Periodic): Flow[Trade, ByteString, Unit] =
    ohlcv.periodic(periodic).via(ohlcv.intervalToRow).via(formatRow)

  private def fetch(symbol: String, transformer: Flow[Trade, ByteString, Any]) =
    handle(tradesClient.history(symbol), transformer)

  private def fetchRaw(symbol: String) =
    handle(tradesClient.rawHistory(symbol), Flow[ByteString])

  private def handle[T](response: BitcoinTradesClient#Response[Source[T, _]], transformer: Flow[T, ByteString, Any]) = {
    response.map[ToResponseMarshallable] {
      // Map to text/plain instead of stream.csv for easy display in browser
      case Right(source) => HttpEntity.Chunked.fromData(`text/plain`, source.via(transformer).via(chunkSize()))
      case Left(err @ (NotFound, _)) => err
      case Left(_) => ServiceUnavailable -> "Service unavailable - error calling an underlying service"
    }
  }
}
