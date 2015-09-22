package bitcoin

import java.io.IOException

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{StatusCode, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import bitcoin.FlowGraphs._

import scala.concurrent.{ExecutionContextExecutor, Future}


trait BitcoinTradesClient {
  type Response[T] = Future[Either[(StatusCode, String), T]]

  def history(symbol: String): Response[Source[Trade, Any]]
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
  val baseUri = Uri(config.getString("service.bitcoin.trades.url"))

  override def history(symbol: String): Response[Source[Trade, Any]] =
    rawHistory(symbol).map(_.right.map(_.via(csv.parse()).via(trade.fromRow)))

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
//      response.status match {
//        case OK => Future.successful(Right(response.entity.dataBytes))
//        case Found | NotFound => Future.successful(
//          Left(NotFound -> s"Invalid symbol or no data found (symbol=$symbol)"))
//        case status => Unmarshal(response.entity).to[String].map { entity =>
//          val error = s"Request to $uri failed with status $status and entity $entity"
//          logger.error(error)
//          Left(status -> error)
//        }
//      }
    }
  }

}