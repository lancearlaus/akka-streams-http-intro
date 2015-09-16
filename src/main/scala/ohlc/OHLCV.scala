package ohlc

import java.time.Instant
import java.util.Date

import scala.math.Ordering.Implicits._

//http://api.bitcoincharts.com/v1/trades.csv?symbol=bitstampUSD

case class Trade(timestamp: Instant, price: Double, amount: Double)

object Trade {
  def apply(unixtime: Long, price: Double, amount: Double) =
    new Trade(Instant.ofEpochSecond(unixtime), price, amount)
}

case class OHLCV (
    open: Double, high: Double, low: Double, close: Double,
    volume: Double,
    count: Long, first: Instant, last: Instant)
{
  def +:(trade: Trade) =
    trade.timestamp match {
      case t if t <= first =>
        OHLCV(trade.price, high.max(trade.price), low.min(trade.price), close, volume + trade.amount, count + 1, t, last)
      case t if t > last =>
        OHLCV(open, high.max(trade.price), low.min(trade.price), trade.price, volume + trade.amount, count + 1, first, t)
      case t =>
        OHLCV(open, high.max(trade.price), low.min(trade.price), close, volume + trade.amount, count + 1, first, last)
    }
  def :+(trade: Trade) =
    trade.timestamp match {
      case t if t < first =>
        OHLCV(trade.price, high.max(trade.price), low.min(trade.price), close, volume + trade.amount, count + 1, t, last)
      case t if t >= last =>
        OHLCV(open, high.max(trade.price), low.min(trade.price), trade.price, volume + trade.amount, count + 1, first, t)
      case t =>
        OHLCV(open, high.max(trade.price), low.min(trade.price), close, volume + trade.amount, count + 1, first, last)
    }
  def isEmpty = volume == 0.0
//  def close(e: Instant) = copy(end = e)
}

object OHLCV {
  def apply(t: Trade) = new OHLCV(t.price, t.price, t.price, t.price, t.amount, 1, t.timestamp, t.timestamp)
//  def apply(price: Double, time: Instant) = new OHLCV(price, price, price, price, 0.0, time, time)
//  def open(close: OHLCV, begin: Option[Instant] = None) = OHLCV(close.close, begin.getOrElse(close.last))
}
