package service.echo

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import service.http.HttpService

// A simple service to demonstrate the HttpService trait
trait EchoService extends HttpService {
  abstract override def route: Route = {
    (get & path("echo" / Segment)) { msg =>
      complete(msg)
    }
  } ~ super.route
}
