package http

import java.time.LocalDate

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes._
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import streams.Flows.appendSma

import scala.concurrent.ExecutionContext

object Main extends App {

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  def history(symbol: String, transformer: Flow[ByteString, ByteString, Any] = Flow[ByteString]) = {
    YahooQuoteService.history(symbol, LocalDate.now.minusMonths(3), LocalDate.now).map[ToResponseMarshallable] {
      // Map to text/plain instead of csv for easy display in browser
      case Some(re) => HttpEntity.Chunked.fromData(ContentTypes.`text/plain`, re.dataBytes.via(transformer))
      case None => NotFound -> s"No data found for the given symbol '$symbol'"
    }
  }

  val route =
    path("hello") {
      get {
        complete {
          "Say hello to akka-http"
        }
      }
    } ~
    pathPrefix("quote" / Segment) { symbol =>
      get {
        pathEnd {
          complete(history(symbol))
        } ~
        path("sma(" ~ IntNumber ~ ")") { window =>
          complete(history(symbol, appendSma(window)))
        }
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  Console.readLine()

  import system.dispatcher // for the future transformations
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ ⇒ system.shutdown()) // and shutdown when done

}
