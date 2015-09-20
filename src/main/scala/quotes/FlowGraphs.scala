package quotes

import akka.stream.OverflowStrategy
import akka.stream.io.Framing
import akka.stream.scaladsl.FlowGraph.Implicits._
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.Fractional.Implicits._

object FlowGraphs {

  object csv {

    // Parse incoming bytes into CSV record stream
    // Note: Each ByteString may contain more (or less) than one line
    def parse(maximumLineLength: Int = 256): Flow[ByteString, Row, Unit] =
      Framing.delimiter(ByteString("\n"), maximumLineLength, allowTruncation = true)
        .map(_.utf8String.trim.split("\\s*,\\s*"))

    // Select a specific column (including header) by name
    def select(name: String): Flow[Row, String, Unit] = Flow[Row]
      .prefixAndTail(1).map { case (header, rows) =>
        header.head.indexOf(name) match {
          case -1    => Source.empty[String]    // Named column not found
          case index => Source.single(name).concatMat(rows.map(_(index)))(Keep.right)
        }
      }.flatten(FlattenStrategy.concat)

    // Convert Array[String] into CSV formatted ByteString
    lazy val format = Flow[Row].map(row => ByteString(row.mkString("", ",", "\n")))

  }

  object calculate {

    // Calculate simple moving average (SMA) using scan()
    // to maintain a running sum and a sliding window
    def sma[T : Fractional](N: Int) = Flow[T]
      .scan((implicitly[Fractional[T]].zero, Seq.empty[T])) {
        case ((sum, win), x) =>
          win.size match {
            case N => (sum + x - win.head, win.tail :+ x)
            case _ => (sum + x,            win      :+ x)
          }
      }
      .drop(N)    // Drop initial and incomplete windows
      .map { case (sum, _) => sum / implicitly[Fractional[T]].fromInt(N) }

  }
  
  object quote {

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
