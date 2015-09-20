package trades

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.{Materializer, ActorMaterializer}
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}
import scala.io.AnsiColor._



object Server extends App with Service {

  override implicit val system = ActorSystem("trades-server")
  override implicit val executor: ExecutionContextExecutor = system.dispatcher
  override implicit val materializer: Materializer = ActorMaterializer()

  override val logger = Logging(system, getClass)
  val config = ConfigFactory.load()

  val interface = config.getString("server.interface")
  val port = config.getInt("server.port")

  print(s"Starting server on $interface:$port...")
  val bindingFuture = Http().bindAndHandle(route, interface, port)

  bindingFuture.onComplete {
    case Success(binding) =>
      println(BOLD + GREEN + s"STARTED" + RESET)
      println(
        s"""
          | Get started with the following URLs:
          |   Hourly OHLCV (Bitstamp USD)  : http://$interface:$port/history/ohlcv/hourly/bitstampUSD
          |   Daily  OHLCV (itBit USD)     : http://$interface:$port/history/ohlcv/daily/itbitUSD
          |   Historic Trades              : http://$interface:$port/history/trades/bitstampUSD
          |   Historic Trades Raw Response : http://$interface:$port/history/trades/bitstampUSD?raw=true
        """.stripMargin.trim)
    case Failure(e) =>
      println(BOLD + RED + "FAILED" + RESET)
      Console.err.println(s"Failed to start server on $interface:$port: ${e.getMessage}")
      system.registerOnTermination(System.exit(1))
      system.shutdown()
  }

}
