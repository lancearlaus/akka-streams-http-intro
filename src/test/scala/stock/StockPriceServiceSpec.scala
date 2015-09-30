package stock

import java.time.LocalDate

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.Uri
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.scalatest.{Matchers, WordSpec}
import support.HttpServiceScalatest

import scala.concurrent.Future

class StockPriceServiceSpec extends WordSpec
    with StockPriceService
    with Matchers
    with HttpServiceScalatest
{

  override lazy val priceClient: StockPriceClient = new StockPriceClient {
    private val data =
      """
        |Date,       Open,  High,  Low,   Close, Volume,  Adj Close
        |2014-12-31, 25.3,  25.3,  24.19, 24.84, 1438600, 24.84
        |2014-12-30, 26.28, 26.37, 25.29, 25.36, 766100,  25.36
        |2014-12-29, 26.64, 26.8,  26.13, 26.42, 619700,  26.42
        |2014-12-26, 27.25, 27.25, 26.42, 26.71, 360400,  26.71
      """.stripMargin.trim

    override def rawHistory(symbol: String, begin: LocalDate, end: LocalDate): Response[Source[ByteString, Any]] = {
      symbol match {
        case "NotFound" => Future.successful(Left(NotFound -> NotFound.defaultMessage))
        case "InternalServiceError" => Future.successful(Left(InternalServerError -> InternalServerError.defaultMessage))
        case "Valid" => Future.successful(Right(Source.single(ByteString(data))))
        case "Fail" => Future.failed(new Exception("test failed future"))
      }
    }
  }

  def makePath(segment: String) = s"/stock/price/daily/$segment"

  "service" should {

    "return data for valid symbol" in {
      Get(Uri(makePath("Valid")).withQuery("calculated" -> "sma(3)")) ~> route ~> check {
        status shouldBe OK
        responseAs[String].split('\n') should have size 3
        responseAs[String] should include("Date")
        responseAs[String] should include("Adj Close")
        responseAs[String] should include("25.36")
        responseAs[String] should not include("26.42")

      }
    }

    "propagate NotFound from client" in {
      Get(makePath("NotFound")) ~> route ~> check {
        status shouldBe NotFound
      }
    }

    "convert other client errors to ServiceUnavailable" in {
      Get(makePath("InternalServiceError")) ~> route ~> check {
        status shouldBe ServiceUnavailable
      }
    }

    "return InternalServerError for failed Future" in {
      Get(makePath("Fail")) ~> route ~> check {
        status shouldBe InternalServerError
      }
    }

  }

}
