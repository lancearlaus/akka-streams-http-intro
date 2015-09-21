package stock

import akka.stream.OverflowStrategy
import akka.stream.scaladsl.FlowGraph.Implicits._
import akka.stream.scaladsl._

import scala.Fractional.Implicits._

object FlowGraphs {

  object calculate {

    // Calculate simple moving average (SMA) using scan()
    // to maintain a running sum and a sliding window
    def sma[A : Fractional](N: Int) = Flow[A]
      .scan((implicitly[Fractional[A]].zero, Seq.empty[A])) {
        case ((sum, win), x) =>
          win.size match {
            case N => (sum + x - win.head, win.tail :+ x)
            case _ => (sum + x,            win      :+ x)
          }
      }
      .drop(N)    // Drop initial and incomplete windows
      .map { case (sum, _) => sum / implicitly[Fractional[A]].fromInt(N) }

  }

  object quote {
    import csv._

    // Calculate and format SMA for a column, renaming the column
    def smaCol(name: String, N: Int, format: String = "%1.2f") = Flow[String]
      .prefixAndTail(1)
      .map { case (header, data) =>
        Source.single(name).concat(
          data.map(_.toDouble)
            .via(calculate.sma((N)))
            .map(_.formatted(format))
        )
      }
      .flatten(FlattenStrategy.concat)

    // Calculate and append SMA column
    def appendSma(N: Int): Flow[Row, Row, Unit] = Flow(
      Broadcast[Row](2),
      Flow[Row].buffer(N, OverflowStrategy.backpressure),
      csv.select("Adj Close"),
      smaCol(s"Adj Close SMA($N)", N),
      ZipWith((row: Row, col: String) => row :+ col)
    )((_, _, _, _, mat) => mat) {
      implicit builder => (bcast, buffer, select, smaCol, append) =>

        bcast ~> buffer           ~> append.in0
        bcast ~> select ~> smaCol ~> append.in1

        (bcast.in, append.out)
    }

  }

}
