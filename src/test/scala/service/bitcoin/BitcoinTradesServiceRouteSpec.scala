package service.bitcoin

import java.io.File
import java.time._
import java.time.format.DateTimeFormatter

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.testkit.RouteTestTimeout
import akka.stream.io.SynchronousFileSource
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.scalatest.{Matchers, WordSpec}
import support.HttpServiceTest

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._

class BitcoinTradesServiceRouteSpec
    extends WordSpec
    with Matchers
    with BitcoinTradesService
    with HttpServiceTest
{

  // Find mock data file (if it exists) for the given symbol
  def mockFile(symbol: String): Option[File] =
    Option(getClass.getResource(s"/mock/bitcoin/trades/$symbol.csv"))
      .map(url => new File(url.getFile))
      .filter(_.exists)

  // Override real client with test client to return known data and response codes
  override lazy val tradesClient = new BitcoinTradesClient {
    override def rawHistory(symbol: String): Response[Source[ByteString, Any]] = {
      symbol match {
        case "NotFoundUSD" => Future.successful(Left(NotFound -> NotFound.defaultMessage))
        case "InternalServerErrorUSD" => Future.successful(Left(InternalServerError -> InternalServerError.defaultMessage))
        case "FailUSD" => Future.failed(new Exception("test failed future"))
        case _ => mockFile(symbol) match {
          case Some(file) => Future.successful(Right(SynchronousFileSource(file)))
          case None => Future.failed(new Exception(s"no test data file found for symbol $symbol"))
        }
      }
    }
  }

  def makePricePath(segments: String*) = "/bitcoin/price/" + segments.mkString("/")
  def makeTradesPath(segments: String*) = "/bitcoin/trades/" + segments.mkString("/")

  "service" should {

    "return daily prices for valid symbol" in {
      implicit val timeout = RouteTestTimeout(3.seconds)

      Get(Uri(makePricePath("daily", "bitstamp", "USD"))) ~> route ~> check {
        val data = responseAs[String]
        val lines = data.split('\n')

        log.info(data)

        status shouldBe OK
        lines should have size 5
        lines.foreach(_.split(',') should have length 7)
        data should include("2015-09-15T00:00:00Z")
        data should include("2015-09-14T00:00:00Z")
        data should include("2015-09-13T00:00:00Z")
        data should include("2015-09-12T00:00:00Z")
      }
    }

    "return daily prices with offset for valid symbol" in {
      implicit val timeout = RouteTestTimeout(3.seconds)

      ZoneId.getAvailableZoneIds.asScala.map(s => ZoneId.of(s).normalized())
        .collect { case offset: ZoneOffset => offset }
        .foreach { offset =>
          val uri = Uri(makePricePath(s"daily($offset)", "bitstamp", "USD"))
          log.info(s"uri: $uri")
          Get(Uri(makePricePath(s"daily($offset)", "bitstamp", "USD"))) ~> route ~> check {
            val data = responseAs[String]
            val lines = data.split('\n')

            log.info(data)

            status shouldBe OK
            lines.size should be >= 4
            lines.size should be <= 5
            Seq(
              "2015-09-15T00:00:00Z",
              "2015-09-14T00:00:00Z",
              "2015-09-13T00:00:00Z"
            ).foreach { dateTime =>
              data should include(OffsetDateTime.parse(dateTime).atZoneSimilarLocal(offset).format(DateTimeFormatter.ISO_INSTANT))
            }
          }
        }
    }

    "return trades for valid symbol" in {
      implicit val timeout = RouteTestTimeout(2.seconds)

      Get(Uri(makeTradesPath("bitstamp", "USD"))) ~> route ~> check {
        val data = responseAs[String]
        val lines = data.split('\n')

        status shouldBe OK
        lines should have size 20001
        data should include(Instant.ofEpochSecond(1442335124L).toString)
        data should include("0.02684316")

      }
    }

    "propagate NotFound from client" in {
      Get(makePricePath("daily", "NotFound", "USD")) ~> route ~> check {
        status shouldBe NotFound
      }
      Get(makeTradesPath("NotFound", "USD")) ~> route ~> check {
        status shouldBe NotFound
      }
    }

    "convert other client errors to ServiceUnavailable" in {
      Get(makePricePath("daily", "InternalServerError", "USD")) ~> route ~> check {
        status shouldBe ServiceUnavailable
      }
      Get(makeTradesPath("InternalServerError", "USD")) ~> route ~> check {
        status shouldBe ServiceUnavailable
      }
    }

    "return InternalServerError for failed Future" in {
      Get(makePricePath("daily", "Fail", "USD")) ~> route ~> check {
        status shouldBe InternalServerError
      }
      Get(makeTradesPath("Fail", "USD")) ~> route ~> check {
        status shouldBe InternalServerError
      }
    }

  }

}
