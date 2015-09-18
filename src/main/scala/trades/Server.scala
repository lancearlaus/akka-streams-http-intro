package trades

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.{Materializer, ActorMaterializer}
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import com.typesafe.config.{ConfigFactory, Config}
import trades.Flows.csv._
import trades.Flows.ohlcv._
import trades.Flows.trade._

import scala.concurrent.{ExecutionContextExecutor, ExecutionContext}

trait Service {

  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit def materializer: Materializer

  def config: Config
  val logger: LoggingAdapter

  def tradesClient: TradesClient = TradesClient

  def fetch(symbol: String, transformer: Flow[ByteString, ByteString, Any] = Flow[ByteString]) = {
    tradesClient.history(symbol).map[ToResponseMarshallable] {
      // Map to text/plain instead of csv for easy display in browser
      case Some(source) => HttpEntity.Chunked.fromData(ContentTypes.`text/plain`, source.via(transformer))
      case None => NotFound -> s"Invalid symbol or no data found for '$symbol'"
    }
  }

  def ohlcv(period: Period): Flow[ByteString, ByteString, Unit] =
    parse().via(fromRow).via(periodic(period)).via(intervalToRow).via(format)

  val route =
    (get & pathPrefix("history")) {
      path("trades" / Segment) { symbol =>
        complete(fetch(symbol))
      } ~
      pathPrefix("ohlcv") {
        path("daily" / Segment) { symbol =>
          complete(fetch(symbol, ohlcv(Daily)))
        } ~
        path("hourly" / Segment) { symbol =>
          complete(fetch(symbol, ohlcv(Hourly)))
        }
      }
    }

}

object Server extends App with Service {

  override implicit val system = ActorSystem("my-system")
  override implicit val executor: ExecutionContextExecutor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)

  val interface = config.getString("server.interface")
  val port = config.getInt("server.port")

  val bindingFuture = Http().bindAndHandle(route, interface, port)

  println(s"Server online at http://$interface:$port/\nPress RETURN to stop...")
  Console.readLine() // for the future transformations
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ ⇒ system.shutdown()) // and shutdown when done

}
