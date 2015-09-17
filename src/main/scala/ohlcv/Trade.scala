package ohlcv

import java.time.Instant

import akka.stream.scaladsl.Flow

// A (very) simple representation of a trade
// See: http://api.bitcoincharts.com/v1/trades.csv?symbol=bitstampUSD
case class Trade(timestamp: Instant, price: Double, amount: Double)

object Trade {
  def apply(unixtime: Long, price: Double, amount: Double) =
    new Trade(Instant.ofEpochSecond(unixtime), price, amount)


  object Flows {

    // Convert from CSV parsed rows to trades
    val rowToTrade = Flow[Array[String]].map { case Array(unixtime, price, amount) =>
      Trade(unixtime.toLong, price.toDouble, amount.toDouble)
    }

  }
}
