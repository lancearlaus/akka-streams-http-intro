package quotes

import java.io.IOException
import java.time.LocalDate

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{StatusCode, ResponseEntity, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import trades.FlowGraphs.{trade, csv}
import trades.Trade

import scala.concurrent.{ExecutionContextExecutor, ExecutionContext, Future}

trait QuotesClient {
  type Response[T] = Future[Either[(StatusCode, String), T]]

  def history(symbol: String, begin: LocalDate, end: LocalDate): Response[Source[Row, Any]]
  def rawHistory(symbol: String, begin: LocalDate, end: LocalDate): Response[Source[ByteString, Any]]
}

case class YahooQuotesClient(implicit
                                     system: ActorSystem,
                                     executor: ExecutionContextExecutor,
                                     materializer: Materializer)
  extends QuotesClient
{
  val logger = Logging(system, getClass)
  val config = ConfigFactory.load()
  val baseUri = Uri(config.getString("service.quotes.history.url"))


  protected def buildUri(symbol: String, begin: LocalDate, end: LocalDate) = {
    val params = Map[String, String](
      "s" -> symbol, "g" -> "d",
      "a" -> (begin.getMonthValue - 1).toString, "b" -> begin.getDayOfMonth.toString, "c" -> begin.getYear.toString,
      "d" -> (end.getMonthValue - 1).toString, "e" -> end.getDayOfMonth.toString, "f" -> end.getYear.toString
    )
    baseUri.withQuery(params)
  }

  override def history(symbol: String, begin: LocalDate, end: LocalDate): Response[Source[Row, Any]] =
    rawHistory(symbol, begin, end).map(_.right.map(_.via(csv.parse())))

  override def rawHistory(symbol: String, begin: LocalDate, end: LocalDate): Response[Source[ByteString, Any]] = {
    val uri = buildUri(symbol, begin, end)

    logger.info(s"Sending request for $uri")

    Http().singleRequest(RequestBuilding.Get(uri)).flatMap { response =>
      logger.info(s"Received response (${response.status}) from $uri")
      response.status match {
        case OK => Future.successful(Right(response.entity.dataBytes))
        case NotFound => Future.successful(Left(
          NotFound -> s"Invalid symbol or no data found (symbol=$symbol, begin=$begin, end=$end)"))
        case status => Unmarshal(response.entity).to[String].flatMap { entity =>
          val error = s"Request to $uri failed with status code $status and entity $entity"
          logger.error(error)
          Future.failed(new IOException(error))
        }
      }
    }

  }

}
