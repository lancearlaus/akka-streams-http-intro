package trades

import java.io.IOException

import akka.actor.ActorSystem
import akka.event.{LoggingAdapter, Logging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{ResponseEntity, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.typesafe.config.{ConfigFactory, Config}

import scala.concurrent.{ExecutionContext, Future}

trait TradesClient {

  val baseUri: Uri

  def history(symbol: String)(implicit system: ActorSystem, materializer: Materializer)
      : Future[Option[Source[ByteString, Any]]] =
  {
    implicit val ec: ExecutionContext = system.dispatcher
    val logger = Logging(system, getClass)

    val uri = baseUri.withQuery(Map("symbol" -> symbol))

    logger.info(s"Sending request for $uri")

    Http().singleRequest(RequestBuilding.Get(uri)).flatMap { response =>
      logger.info(s"received response from $uri")
      response.status match {
        case OK => Future.successful(Some(response.entity.dataBytes))
        case NotFound => Future.successful(None)
        case _ => Unmarshal(response.entity).to[String].flatMap { entity =>
          val error = s"Request to $uri failed with status code ${response.status}"
          Future.failed(new IOException(error))
        }
      }
    }
  }

}

object TradesClient extends TradesClient {

  val config = ConfigFactory.load()

  override val baseUri = Uri(config.getString("service.trades.history.url"))
//  override val baseUri = Uri("http://api.bitcoincharts.com/v1/trades.csv")

}
