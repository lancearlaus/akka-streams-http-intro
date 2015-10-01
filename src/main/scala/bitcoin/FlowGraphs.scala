package bitcoin

import java.time.ZoneId

import akka.stream.scaladsl.FlowGraph.Implicits._
import akka.stream.scaladsl._

object FlowGraphs {

  object trade {
    import csv._

    // Convert CSV parsed rows to trades
    lazy val fromRow = Flow[Row].map { case Array(unixtime, price, amount) =>
      Trade(unixtime.toLong, price.toDouble, amount.toDouble)
    }

    // Convert trades to CSV rows
    def toRow(addHeader: Boolean = true): Flow[Trade, Row, Unit] = {
      val noHeader = Flow[Trade]
        .map(t => Array(t.timestamp.toString, t.price.toString, t.amount.toString))
      addHeader match {
        case false => noHeader
        case true => noHeader.prefixAndTail(0).map { case (_, tail) =>
          Source.single(Array("timestamp", "price", "amount"))
            .concatMat(tail)(Keep.right)
        }.flatten(FlattenStrategy.concat)
      }
    }

  }

  object period {

    // Split a stream of time ordered trades into sub-streams by interval
    // Each sub-stream emits (Interval, Trade) elements
    def intervals(period: Periodic) = Flow[Trade]
      .scan((Interval.empty, Interval.empty, null.asInstanceOf[Trade])) {
        case ((_, cur, _), trade) =>
          (cur.contains(trade.timestamp)) match {
            case true => (cur, cur, trade)
            case false => (cur, period.interval(trade.timestamp), trade)
          }
      }
      .drop(1)
      .splitWhen { case (last, cur, _) => last != cur }
      .map(_.map { case (_, cur, trade) => (cur, trade) })

  }

  object ohlcv {
    import csv._

    // Calculate OHLCV aggregate for a stream of trades
    lazy val aggregate = Flow[Trade]
      .fold(Option.empty[OHLCV]) { case (ohlcv, trade) =>
        ohlcv.map(trade +: _).orElse(Some(OHLCV(trade)))
      }
      .collect { case Some(ohlcv) => ohlcv }

    // Calculate OHLCV aggregate for a trade interval
    lazy val aggregateInterval: Flow[(Interval, Trade), (Interval, OHLCV), Unit] =
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
    def periodic(p: Periodic) = Flow[Trade]
      .via(period.intervals(p).map(_.via(aggregateInterval)))
      .flatten(FlattenStrategy.concat)

    private def formatValue(x: Any): String = x match {
      case d: Double => d.formatted("%1.2f")
      case _ => x.toString
    }

    // Convert interval OHLCVs to CSV-ready row stream with header
    lazy val intervalToRow: Flow[(Interval, OHLCV), Row, Unit] =
      Flow[(Interval, OHLCV)]
        .scan(Array("Begin", "End", "Open", "High", "Low", "Close", "Volume")) {
        case (_, (interval, ohlcv)) => Array(
          formatValue(interval.begin),
          formatValue(interval.end),
          formatValue(ohlcv.open),
          formatValue(ohlcv.high),
          formatValue(ohlcv.low),
          formatValue(ohlcv.close),
          formatValue(ohlcv.volume)
        )
      }

  }

}
