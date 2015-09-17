package ohlcv

import java.time.{LocalDate, OffsetDateTime}

import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.util.ByteString
import http.TradesClient
import ohlcv.Flows._
import ohlcv.Trade.Flows._
import streams.Flows._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Random

object Main extends App {

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  def trades(symbol: String, transformer: Flow[ByteString, ByteString, Any] = Flow[ByteString]) = {
    TradesClient.history(symbol).map[ToResponseMarshallable] {
      // Map to text/plain instead of csv for easy display in browser
      case Some(re) => HttpEntity.Chunked.fromData(ContentTypes.`text/plain`, re.dataBytes.via(transformer))
      case None => NotFound -> s"No data found for the given symbol '$symbol'"
    }
  }

  def ohlcvCsv(period: Period) =
    parseCsv.via(rowToTrade).via(periodicOHLCV(period)).via(intervalOHLCVtoRow).via(csv)

  val route =
    pathPrefix("trades") {
      path("history" / Segment) { symbol =>
        get { complete(trades(symbol)) }
      } ~
      pathPrefix("history" / "ohlcv") {
        path("daily" / Segment) { symbol =>
          get { complete(trades(symbol, ohlcvCsv(Daily))) }
        } ~
        path("hourly" / Segment) { symbol =>
          get { complete(trades(symbol, ohlcvCsv(Hourly))) }
        }
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  Console.readLine() // for the future transformations
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ ⇒ system.shutdown()) // and shutdown when done

}
