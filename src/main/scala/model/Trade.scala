package model

import java.time.Instant

// A (very) simple representation of a trade
// See: http://api.bitcoincharts.com/v1/trades.csv?symbol=bitstampUSD
case class Trade(timestamp: Instant, price: Double, amount: Double)

object Trade {
  def apply(unixtime: Long, price: Double, amount: Double) =
    new Trade(Instant.ofEpochSecond(unixtime), price, amount)

  def apply(price: Double, amount: Double) =
    new Trade(Instant.now(), price, amount)
}
