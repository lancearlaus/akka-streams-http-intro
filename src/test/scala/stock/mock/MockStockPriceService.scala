package stock.mock

import java.io.File

import akka.event.Logging
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.io.SynchronousFileSource
import http.HttpService

trait MockStockPriceService extends HttpService {

  private lazy val log = Logging(system, classOf[MockStockPriceService])

  // Find mock data file (if it exists) for the given symbol
  private def mockFile(symbol: String): Option[File] =
    Option(getClass.getResource(s"/mock/stock/price/$symbol.csv"))
      .map(url => new File(url.getFile))
        .filter(_.exists)

  abstract override def route: Route =
    (get & path("stock"/"price"/"daily" / Segment)) { (symbol) =>
      log.info(s"Received request for $symbol")
      complete {
        mockFile(symbol) match {
          case Some(file) => {
            log.info(s"Responding with file ${file.getCanonicalPath}")
            // Use text/plain for easy browser display
            HttpEntity.Chunked.fromData(`text/plain`, SynchronousFileSource(file))
          }
          case None => NotFound
        }
      }
    } ~ super.route

}
