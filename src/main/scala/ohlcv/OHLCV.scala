package ohlcv

import java.time.Instant

import scala.math.Ordering.Implicits._


// Open-High-Low-Close-Volume trade aggregation
case class OHLCV (
    open: Double, high: Double, low: Double, close: Double,
    volume: Double,
    count: Long, first: Instant, last: Instant)
{

  // Prepend a trade
  def +:(trade: Trade) =
    trade.timestamp match {
      case t if t <= first =>
        OHLCV(trade.price, high.max(trade.price), low.min(trade.price), close, volume + trade.amount, count + 1, t, last)
      case t if t > last =>
        OHLCV(open, high.max(trade.price), low.min(trade.price), trade.price, volume + trade.amount, count + 1, first, t)
      case t =>
        OHLCV(open, high.max(trade.price), low.min(trade.price), close, volume + trade.amount, count + 1, first, last)
    }

  // Append a trade
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
}

object OHLCV {
  def apply(t: Trade) = new OHLCV(t.price, t.price, t.price, t.price, t.amount, 1, t.timestamp, t.timestamp)
}
