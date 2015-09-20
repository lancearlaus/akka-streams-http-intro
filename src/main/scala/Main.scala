import http.{EchoService, HttpServer}
import quotes.QuotesService

import scala.util.Success

object Main extends App with HttpServer with EchoService with QuotesService {

  bindingFuture.andThen { case Success(_) =>
    println(
      s"""
         | Get started with the following URLs:
         |   Hourly OHLCV (Bitstamp USD)  : http://$interface:$port/history/ohlcv/hourly/bitstampUSD
         |   Daily  OHLCV (itBit USD)     : http://$interface:$port/history/ohlcv/daily/itbitUSD
         |   Historic Trades              : http://$interface:$port/history/trades/bitstampUSD
         |   Historic Trades Raw Response : http://$interface:$port/history/trades/bitstampUSD?raw=true
      """.stripMargin.trim)
  }
}
