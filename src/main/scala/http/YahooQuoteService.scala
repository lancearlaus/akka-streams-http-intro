package http

import java.io.IOException
import java.time.LocalDate

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{ResponseEntity, HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString

import scala.concurrent.{ExecutionContext, Future}

object YahooQuoteService {

  val baseUri = Uri("http://real-chart.finance.yahoo.com/table.csv")

  protected def queryParams(symbol: String, begin: LocalDate, end: LocalDate) =
    Map[String, String](
      "s" -> symbol, "g" -> "d",
      "a" -> (begin.getMonthValue - 1).toString, "b" -> begin.getDayOfMonth.toString, "c" -> begin.getYear.toString,
      "d" -> (end.getMonthValue - 1).toString, "e" -> end.getDayOfMonth.toString, "f" -> end.getYear.toString
    )

  def history(symbol: String, begin: LocalDate, end: LocalDate)(implicit system: ActorSystem, materializer: Materializer): Future[Option[ResponseEntity]] = {
    require(end.isAfter(begin) || end.isEqual(begin), "invalid date range - end date must be on or after begin date")

    implicit val ec: ExecutionContext = system.dispatcher
    val uri = baseUri.withQuery(queryParams(symbol, begin, end))

    //logger.info(s"Sending request for $uri")

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

//    client.request(RequestBuilding.Get(uri)).flatMap { response =>
//      logger.info(s"Received response with status ${response.status} from $uri")
//    }
  }


}
