package ohlc

import java.time.Instant
import java.time.temporal.ChronoUnit

import akka.stream.scaladsl.{FlattenStrategy, Flow}

import scala.math.Ordering.Implicits._

object Flows {


  // A half-open time interval [begin, end)
  case class Interval(begin: Instant, end:Instant) {
    def contains(instant: Instant) = (begin <= instant) && (instant < end)
  }
  object Interval {
    val empty = Interval(Instant.MIN, Instant.MIN)
  }

  sealed trait Period {
    def interval(instant: Instant): Interval
  }
  case object Daily extends Period {
    override def interval(instant: Instant) = {
      val begin = instant.truncatedTo(ChronoUnit.DAYS)
      val end = begin.plus(1, ChronoUnit.DAYS)
      Interval(begin, end)
    }
  }


  // Convert from rows to trades
  val trade = Flow[Array[String]].map { case Array(unixtime, price, amount) =>
    Trade(unixtime.toLong, price.toDouble, amount.toDouble)
  }

  // Split the stream of trades into a stream of streams by interval
  // Each stream will emit (Interval, Trade) Tuples
  def split(period: Period) = Flow[Trade]
    .scan((Interval.empty, Interval.empty, null.asInstanceOf[Trade])) {
      case ((_, last, _), trade) => (last.contains(trade.timestamp)) match {
        case true => (last, last, trade)
        case false => (last, period.interval(trade.timestamp), trade)
      }
    }
    .drop(1)
    .splitWhen { case (last, cur, _) => last != cur }
    .map(_.map { case (_, cur, trade) => (cur, trade) })

  // Calculate OHLCV aggregate for a stream of trades
  val aggregate = Flow[(Interval, Trade)]
    .fold((null.asInstanceOf[Interval], Option.empty[OHLCV])) {
      case ((_, ohlcv), (interval, trade)) =>
        (interval, ohlcv.map(trade +: _).orElse(Some(OHLCV(trade))))
    }
    .map { case (interval, Some(ohlcv)) => (interval, ohlcv) }

  def ohlcv(period: Period) = Flow[Trade]
    .via(split(period).map(_.via(aggregate)))
    .flatten(FlattenStrategy.concat)

}
