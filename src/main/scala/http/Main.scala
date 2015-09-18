package http

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
import quotes.Flows._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Random

object Main extends App {

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  def history(symbol: String, transformer: Flow[ByteString, ByteString, Any] = Flow[ByteString]) = {
    QuotesClient.history(symbol, LocalDate.now.minusMonths(3), LocalDate.now).map[ToResponseMarshallable] {
      // Map to text/plain instead of csv for easy display in browser
      case Some(re) => HttpEntity.Chunked.fromData(ContentTypes.`text/plain`, re.dataBytes.via(transformer))
      case None => NotFound -> s"No data found for the given symbol '$symbol'"
    }
  }

  def rangedRandom(floor: Double, range: Double) = (floor + (Random.nextDouble() * range))

  // Stream a random quote every second
  val streamingQuoteSource: Source[Message, Cancellable] = Source.concatMat(
      Source.single(Array("Date", "Adj Close").mkString(",")),
      Source(0.seconds, 1.second, Unit)
        .map(_ => Array(OffsetDateTime.now().toString, rangedRandom(20, 10).formatted("%1.2f")))
        .via(csv)
        .map(_.utf8String)
    )(Keep.right)
    .map(row => TextMessage(row))

  val streamingQuoteSink = Flow[Message].to(Sink.foreach(m => println(s"received: $m")))

  val streamingQuoteService: Flow[Message, Message, _] =
    Flow.wrap(streamingQuoteSink, streamingQuoteSource)(Keep.right)

  val symbols = Flow[String]
    .map(_.trim.split("\\s*,\\s*"))


  val route =
    path("hello") {
      get {
        complete {
          "Say hello to akka-http"
        }
      }
    } ~
    pathPrefix("quote") {
      path("history" / Segment) { symbol =>
        get { complete(history(symbol)) }
      } ~
      path("history" / Segment / "sma(" ~ IntNumber ~ ")") { (symbol, window) =>
        get { complete(history(symbol, appendSma(window))) }
      } ~
      path("stream") {
        get { handleWebsocketMessages(streamingQuoteService) }
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  Console.readLine() // for the future transformations
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ ⇒ system.shutdown()) // and shutdown when done

}
