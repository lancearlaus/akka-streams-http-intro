package trades

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes.NotFound
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.Flow
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.ByteString
import com.typesafe.config.{Config, ConfigFactory}
import trades.Flows.csv._
import trades.Flows.ohlcv._
import trades.Flows.trade._

import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContextExecutor

trait Service {

  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit def materializer: Materializer

  val logger: LoggingAdapter

  lazy val client: TradeHistoryClient = CoinbaseTradesClient()

  def fetch(symbol: String, transformer: Flow[Trade, ByteString, Any] = Flow[Trade].via(toRow).via(format)) = {
    client.history(symbol).map[ToResponseMarshallable] {
      // Map to text/plain instead of csv for easy display in browser
//      case Some(source) => HttpEntity.empty(ContentTypes.`text/plain`)
      case Some(source) => HttpEntity.Chunked.fromData(ContentTypes.`text/plain`, source.via(transformer))
      case None => NotFound -> s"Invalid symbol or no data found for '$symbol'"
    }
  }

  def ohlcv(period: Period): Flow[Trade, ByteString, Unit] =
    periodic(period).via(intervalToRow).via(format)

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

  override val logger = Logging(system, getClass)
  val config = ConfigFactory.load()

  val interface = config.getString("server.interface")
  val port = config.getInt("server.port")

  println(s"Starting server on $interface:$port...")
  val bindingFuture = Http().bindAndHandle(route, interface, port)

  bindingFuture.onComplete {
    case Success(binding) =>
      println(s"Server online at http://$interface:$port/")
    case Failure(e) =>
      Console.err.println(s"Failed to start server on $interface:$port: ${e.getMessage}")
      system.registerOnTermination(System.exit(1))
      system.shutdown()
  }

}
