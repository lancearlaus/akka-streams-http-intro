package bitcoin

import java.time.{Instant, LocalDate}

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.Uri
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.scalatest.{Matchers, WordSpec}
import support.HttpServiceTest

import scala.concurrent.Future

class BitcoinTradesServiceRouteSpec
    extends WordSpec
    with Matchers
    with BitcoinTradesService
    with HttpServiceTest
{

  override lazy val tradesClient = new BitcoinTradesClient {
    private val csv =
      """
        |1442335124,230.140000000000,0.100069520000
        |1442335100,230.140000000000,0.247311250000
        |1442334912,229.810000000000,4.887990000000
        |1442334896,230.140000000000,0.247406130000
        |1442334784,229.820000000000,2.130000000000
      """.stripMargin.trim

    override def rawHistory(symbol: String): Response[Source[ByteString, Any]] = {
      symbol match {
        case "NotFoundUSD" => Future.successful(Left(NotFound -> NotFound.defaultMessage))
        case "InternalServerErrorUSD" => Future.successful(Left(InternalServerError -> InternalServerError.defaultMessage))
        case "ValidUSD" => Future.successful(Right(Source.single(ByteString(csv))))
        case "FailUSD" => Future.failed(new Exception("test failed future"))
      }

    }

  }

  def makePricePath(segments: String*) = "/bitcoin/price/daily/" + segments.mkString("/")
  def makeTradesPath(segments: String*) = "/bitcoin/trades/" + segments.mkString("/")

  "service" should {

    "return trades for valid symbol" in {
      Get(Uri(makeTradesPath("Valid", "USD"))) ~> route ~> check {
        val data = responseAs[String]
        val lines = data.split('\n')

        status shouldBe OK
        lines should have size 6
        data should include(Instant.ofEpochSecond(1442335124L).toString)
        data should include("2.13")

      }
    }

    "propagate NotFound from client" in {
      Get(makeTradesPath("NotFound", "USD")) ~> route ~> check {
        status shouldBe NotFound
      }
    }

    "convert other client errors to ServiceUnavailable" in {
      Get(makeTradesPath("InternalServerError", "USD")) ~> route ~> check {
        status shouldBe ServiceUnavailable
      }
    }

    "return InternalServerError for failed Future" in {
      Get(makeTradesPath("Fail", "USD")) ~> route ~> check {
        status shouldBe InternalServerError
      }
    }

  }

}
