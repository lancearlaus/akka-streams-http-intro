package service.bitcoin

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{StatusCode, Uri}
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import model.Trade
import service.bitcoin.FlowGraphs._
import stream.csv

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}


trait BitcoinTradesClient {
  type Response[T] = Future[Either[(StatusCode, String), T]]

  // Parses raw history stream as CSV and converts to Trades
  def history(symbol: String)(implicit ec: ExecutionContext): Response[Source[Trade, Any]] =
    rawHistory(symbol).map(_.right.map(_.via(csv.parseCsv()).via(trade.fromRow)))

  def rawHistory(symbol: String): Response[Source[ByteString, Any]]
}

case class BitcoinChartsTradesClient(implicit
    system: ActorSystem,
    executor: ExecutionContextExecutor,
    materializer: Materializer)
  extends BitcoinTradesClient
{
  val log = Logging(system, getClass)
  val config = ConfigFactory.load()
  val baseUri = Uri(config.getString("service.service.bitcoin.trades.url"))

  override def rawHistory(symbol: String): Response[Source[ByteString, Any]] = {
    val uri = baseUri.withQuery(Map("symbol" -> symbol))

    log.info(s"Sending request for $uri")

    Http().singleRequest(RequestBuilding.Get(uri)).map { response =>
      log.info(s"Received response (${response.status}) from $uri")
      response.status match {
        case OK               => Right(response.entity.dataBytes)
        case Found | NotFound => Left(NotFound -> s"Invalid symbol or no data found (symbol=$symbol)")
        case status           => Left(status -> s"Request to $uri failed with status $status")
      }

    }
  }

}