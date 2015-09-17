package ohlcv

import akka.stream.scaladsl.FlowGraph.Implicits._
import akka.stream.scaladsl._

object Flows {

  // Convert from CSV parsed rows to trades
  val trade = Flow[Array[String]].map { case Array(unixtime, price, amount) =>
    Trade(unixtime.toLong, price.toDouble, amount.toDouble)
  }

  // Calculate OHLCV aggregate for a stream of trades
  val aggregate = Flow[Trade]
    .fold(Option.empty[OHLCV]) {
      case (ohlcv, trade) => ohlcv.map(trade +: _).orElse(Some(OHLCV(trade)))
    }
    .collect { case Some(ohlcv) => ohlcv }

  // Split the stream of trades into a stream of streams by interval
  // Each sub-stream emits (Interval, Trade) elements
  def intervals(period: Period) = Flow[Trade]
    .scan((Interval.empty, Interval.empty, null.asInstanceOf[Trade])) {
      case ((_, last, _), trade) =>
        (last.contains(trade.timestamp)) match {
          case true => (last, last, trade)
          case false => (last, period.interval(trade.timestamp), trade)
        }
    }
    .drop(1)
    .splitWhen { case (last, cur, _) => last != cur }
    .map(_.map { case (_, cur, trade) => (cur, trade) })

  // Calculate trade interval OHLCV aggregate
  val intervalOHLCV: Flow[(Interval, Trade), (Interval, OHLCV), Unit] =
    Flow(
      Unzip[Interval, Trade](),
      Flow[Interval].take(1),
      aggregate,
      Zip[Interval, OHLCV]
    )((_, _, _, mat) => mat) {
      implicit builder => (unzip, interval, aggregate, zip) =>

        unzip.out0 ~> interval  ~> zip.in0
        unzip.out1 ~> aggregate ~> zip.in1

        (unzip.in, zip.out)
    }

  // Calculate the periodic OHLCV for a stream of trades
  def periodicOHLCV(period: Period) = Flow[Trade]
    .via(intervals(period).map(_.via(intervalOHLCV)))
    .flatten(FlattenStrategy.concat)

  def format(x: Any): String = x match {
    case d: Double => d.formatted("%1.2f")
    case _ => x.toString
  }

  val intervalOHLCVtoRow: Flow[(Interval, OHLCV), Array[String], Unit] =
    Flow[(Interval, OHLCV)]
      .scan(Array("Begin", "End", "Open", "High", "Low", "Close", "Volume")) {
      case (_, (interval, ohlcv)) => Array(
        format(interval.begin),
        format(interval.end),
        format(ohlcv.open),
        format(ohlcv.high),
        format(ohlcv.low),
        format(ohlcv.close),
        format(ohlcv.volume)
      )
    }
}
