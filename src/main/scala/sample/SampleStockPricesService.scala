package sample

import java.io.File

import akka.event.Logging
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.stream.io.SynchronousFileSource
import http.HttpService

trait SampleStockPricesService extends HttpService {

  private lazy val log = Logging(system, classOf[SampleStockPricesService])

  // Find sample file (if it exists) for the given symbol
  private def sampleFile(symbol: String): Option[File] =
    Option(getClass.getResource(s"/sample/prices/$symbol.csv"))
      .flatMap(url => Option(new File(url.getFile)))
        .filter(_.exists)

  // http://localhost/sample/prices/daily/yhoo
  abstract override def route: Route =
    (get & path("sample"/"prices"/"daily" / Segment)) { (symbol) =>
      log.info(s"Received request for $symbol")
      complete {
        sampleFile(symbol) match {
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
