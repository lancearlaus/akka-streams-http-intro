package quotes

import java.time.{LocalDate, Period}

import akka.event.Logging
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString
import http.FlowGraphs.chunk
import http.HttpService
import quotes.FlowGraphs._

trait QuotesService extends HttpService {

  lazy val logger = Logging(system, classOf[QuotesService])
  lazy val client: QuotesClient = YahooQuotesClient()

  def handle[T](response: QuotesClient#Response[Source[T, _]], transformer: Flow[T, ByteString, Any]) = {
    response.map[ToResponseMarshallable] {
      case Right(source) => HttpEntity.Chunked
        // Map to text/plain instead of csv for easy display in browser
        .fromData(ContentTypes.`text/plain`, source.via(transformer).via(chunk.min(2048)))
      case Left(err@(NotFound, _)) => err
      case Left(_) => ServiceUnavailable -> "Service unavailable - error calling an underlying service"
    }
  }

  def fetch(symbol: String, period: Period, transformer: Flow[Row, ByteString, Any]) = {
    val now = LocalDate.now()
    handle(client.history(symbol, now.minus(period), now), transformer)
  }

  def fetchRaw(symbol: String, period: Period) = {
    val now = LocalDate.now()
    handle(client.rawHistory(symbol, now.minus(period), now), Flow[ByteString])
  }

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

  def calculatedColumns(columns: String*): Either[String, Flow[Row, Row, Unit]] =
    columns.foldLeft[Either[String, Flow[Row, Row, Unit]]](Right(Flow[Row])) {
      case (Right(flow), col) => col match {
        case Sma(win) => Right(flow.via(quote.appendSma(win.toInt)))
        case _ => Left(s"Invalid calculated column $col")
      }
      case (err @ Left(_), _) => err
    }

  // Parse a string as a period, assuming no "P" prefix
  implicit val periodFromStringUnmarshaller: Unmarshaller[String, Period] =
    Unmarshaller.strict[String, Period](s => Period.parse("P" + s))

  val DefaultPeriod = Period.ofMonths(3)

  abstract override def route =
    (get & pathPrefix("quotes" / "history")) {
      (path("raw" / Segment) & parameter('period.as[Period] ? DefaultPeriod)) { (symbol, period) =>
        complete(fetchRaw(symbol, period))
      } ~
      (path(Segment) & parameters('period.as[Period] ? DefaultPeriod, 'calculated ? "sma(10)")) {
        (symbol, period, calculated) =>
          calculatedColumns(calculated.split(',') : _*) match {
            case Right(flow) => complete(fetch(symbol, period, flow.via(csv.format)))
            case Left(msg) => complete(BadRequest -> msg)
          }
      }
    } ~ super.route

}
