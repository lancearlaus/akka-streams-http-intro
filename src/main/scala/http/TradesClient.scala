package http

import java.io.IOException

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{ResponseEntity, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer

import scala.concurrent.{ExecutionContext, Future}

object TradesClient {

  val baseUri = Uri("http://api.bitcoincharts.com/v1/trades.csv")

  def history(symbol: String)
             (implicit system: ActorSystem, materializer: Materializer)
      : Future[Option[ResponseEntity]] = {

    implicit val ec: ExecutionContext = system.dispatcher

    val uri = baseUri.withQuery(Map("symbol" -> symbol))

    println(s"Sending request for $uri")

    Http().singleRequest(RequestBuilding.Get(uri)).flatMap { response =>
      response.status match {
        case OK => Future.successful(Some(response.entity))
        case NotFound => Future.successful(None)
        case _ => Unmarshal(response.entity).to[String].flatMap { entity =>
          val error = s"Request to $uri failed with status code ${response.status}"
          //          logger.error(error)
          Future.failed(new IOException(error))
        }
      }
    }
  }
}
