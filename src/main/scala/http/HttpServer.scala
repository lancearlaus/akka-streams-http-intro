package http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.{Materializer, ActorMaterializer}
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContextExecutor
import scala.io.AnsiColor._
import scala.util.{Failure, Success}

/**
 * Trait used to implement an Akka HTTP server that provides one or more services.
 *
 * Usage:
 * 1. Extend the [HttpService] trait to implement the route for your service. See the [HttpService] trait
 *    for more information.
 * 2. Create an application class that extends the Scala `App` class, the `HttpServer` trait, and the
 *    `HttpService`-derived trait(s).
 *
 * Example:
 * {{{
 *  // Define the service route in trait
 *  trait EchoService extends HttpService {
 *    abstract override def route: Route = {
 *      (get & path("echo" / Segment)) { msg =>
 *          complete(msg)
 *      }
 *    } ~ super.route
 *  }
 *
 *  // Implement the trait in your application class
 *  object Main extends App with HttpServer with EchoService
 *
 * }}}
 */
trait HttpServer {

  protected implicit val system = ActorSystem("http-server")
  protected implicit val executor: ExecutionContextExecutor = system.dispatcher
  protected implicit val materializer: Materializer = ActorMaterializer()

  protected val config = ConfigFactory.load()

  def interface = config.getString("http.server.interface")
  def port      = config.getInt("http.server.port")

  // Route defined in service class
  def route: Route

  print(s"Starting server on $interface:$port...")
  val bindingFuture = Http().bindAndHandle(route, interface, port)
    .andThen {
      case Success(binding) =>
        println(BOLD + GREEN + s"STARTED" + RESET)
      case Failure(e) =>
        println(BOLD + RED + "FAILED" + RESET)
        Console.err.println(s"Failed to start server on $interface:$port: ${e.getMessage}")
        system.registerOnTermination(System.exit(1))
        system.shutdown()
    }

}
