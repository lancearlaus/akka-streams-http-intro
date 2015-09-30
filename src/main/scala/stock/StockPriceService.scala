package stock

import java.time.{LocalDate, Period}

import akka.event.Logging
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpEntity, MediaTypes}
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString
import csv._
import http._
import stock.FlowGraphs._
import stock.PeriodConversions._

trait StockPriceService extends HttpService {

  private lazy val log = Logging(system, classOf[StockPriceService])
  protected lazy val priceClient: StockPriceClient = YahooStockPriceClient()

  def defaultPeriod: Period = config.getString("service.stocks.period.default")

  abstract override def route =
    (get & pathPrefix("stock"/"price"/"daily")) {
      (path(Segment) & parameters('raw.as[Boolean] ! true, 'period.as[Period] ? defaultPeriod)) { (symbol, period) =>
        complete(fetchRaw(symbol, period))
      } ~
      (path(Segment) & parameters('period.as[Period] ? defaultPeriod, 'calculated ? "sma(10)")) {
        (symbol, period, calculated) =>
          calculatedColumns(calculated.split(',') : _*) match {
            case Left(msg) => complete(BadRequest -> msg)
            case Right(flow) => complete(fetch(symbol, period, flow.via(csv.format)))
          }
      }
    } ~ super.route

  private def fetch(symbol: String, period: Period, transformer: Flow[Row, ByteString, Any]) = {
    val now = LocalDate.now()
    handle(priceClient.history(symbol, now.minus(period), now), transformer)
  }

  private def fetchRaw(symbol: String, period: Period) = {
    val now = LocalDate.now()
    handle(priceClient.rawHistory(symbol, now.minus(period), now), Flow[ByteString])
  }

  private def handle[T](response: StockPriceClient#Response[Source[T, _]], transformer: Flow[T, ByteString, Any]) = {
    response.map[ToResponseMarshallable] {
      // Map to text/plain instead of csv for easy display in browser
      case Right(source) => HttpEntity.Chunked.fromData(`text/plain`, source.via(transformer).via(chunk.min(2048)))
      case Left(err @ (NotFound, _)) => err
      case Left(_) => ServiceUnavailable -> "Service unavailable - error calling an underlying service"
    }
  }

  private def calculatedColumns(columns: String*): Either[String, Flow[Row, Row, Unit]] =
    columns.foldLeft[Either[String, Flow[Row, Row, Unit]]](Right(Flow[Row])) {
      case (Right(flow), col) => col match {
        case Sma(win) => Right(flow.via(quote.appendSma(win)))
        case _ => Left(s"Invalid calculated column $col")
      }
      case (err @ Left(_), _) => err
    }

  // Extractor for sma() calculated column parameter
  object Sma {
    val Pattern = "sma\\((\\d+)\\)".r

    // SMA column window size extractor
    // Example: "sma(30)" => 30
    def unapply(col: String): Option[Int] = {
      try {
        col match {
          case Pattern(size) => size.toInt match {
            case n if n > 0 => Some(n)
            case _ => None
          }
          case _ => None
        }
      } catch {
        case e: NumberFormatException => None
      }
    }
  }


}
