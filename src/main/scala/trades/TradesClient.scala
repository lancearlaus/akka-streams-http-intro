package trades

import java.io.IOException

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.typesafe.config.ConfigFactory
import trades.Flows._

import scala.concurrent.{ExecutionContextExecutor, Future}


trait TradeHistoryClient {
  def history(symbol: String): Future[Option[Source[Trade, Any]]]
}

case class CoinbaseTradesClient(implicit system: ActorSystem, materializer: Materializer) extends TradeHistoryClient {
  implicit val executor: ExecutionContextExecutor = system.dispatcher
  val logger = Logging(system, getClass)
  val config = ConfigFactory.load()
  val baseUri = Uri(config.getString("service.trades.history.url"))

  override def history(symbol: String): Future[Option[Source[Trade, Any]]] = {
    val uri = baseUri.withQuery(Map("symbol" -> symbol))

    logger.info(s"Sending request for $uri")

    Http().singleRequest(RequestBuilding.Get(uri)).flatMap { response =>
      logger.info(s"Received response (${response.status}) from $uri")
      response.status match {
        case OK => Future.successful(Some(response.entity.dataBytes.via(csv.parse()).via(trade.fromRow)))
        case NotFound => Future.successful(None)
        case _ => Unmarshal(response.entity).to[String].flatMap { entity =>
          val error = s"Request to $uri failed with status code ${response.status}"
          Future.failed(new IOException(error))
        }
      }
    }
  }

}
//object CoinbaseTradesClient {
//  def apply()(implicit system: ActorSystem, materializer: Materializer) = new CoinbaseTradesClient()
//}
