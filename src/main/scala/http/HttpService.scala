package http

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives.reject
import akka.stream.Materializer
import com.typesafe.config.Config


import scala.concurrent.ExecutionContextExecutor

/**
 * Base trait used to define services. Defines the service route used by [HttpServer].
 *
 * This trait is designed to be stackable, allowing multiple services to be implemented in a single server.
 *
 * Usage: Extend this trait and override the route function to create your service.
 *
 * Example: Note the use of `abstract override` and the concatenation of `super.route` to make the service stackable.
 * {{{
 *
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
trait HttpService {

  protected implicit val system: ActorSystem
  protected implicit def executor: ExecutionContextExecutor
  protected implicit def materializer: Materializer
  protected def config: Config

  // Services must supply service route by overriding this function
  def route: Route = reject
}
