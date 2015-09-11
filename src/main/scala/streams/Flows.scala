package streams

import akka.stream.io.Framing
import akka.stream.scaladsl._
import akka.util.ByteString
import akka.stream.scaladsl.FlowGraph.Implicits._

object Flows {

  // Calculate simple moving average (SMA) using scan()
  // to maintain a running sum and a sliding window
  def sma(N: Int) = Flow[Double]
    .scan((0.0, Seq.empty[Double])) { case ((sum, win), x) =>
      win.size match {
        case N => (sum + x - win.head, win.tail :+ x)
        case _ => (sum + x,            win      :+ x)
      }
    }
    .drop(N)    // Drop initial and incomplete windows
    .map { case (sum, _) => sum / N }

  
  // Parse incoming bytes into CSV record stream
  // Note: Each ByteString may contain more (or less) than one line
  val parse: Flow[ByteString, Array[String], Unit] = Framing
    .delimiter(ByteString("\n"), maximumFrameLength = 256, allowTruncation = true)
    .map(_.utf8String.split("\\s*,\\s*"))
    .log("parse", _.mkString(","))


  // Select a specific column (including header) by name
  def select(name: String): Flow[Array[String], String, Unit] = Flow[Array[String]]
    .prefixAndTail(1).map { case (header, rows) =>
    header.head.indexOf(name) match {
      case -1    => throw new Exception(s"column $name not found")
      case index => Source.single(name).concatMat(rows.map(_(index)))(Keep.right)
    }
  }.flatten(FlattenStrategy.concat)


  // Convert Array[String] into CSV formatted ByteString
  val csv = Flow[Array[String]].map(row => ByteString(row.mkString("", ",", "\n")))


  // Drop header and calculate SMA
  def calc(n: Int) = Flow[String].drop(1).map(_.toDouble).via(sma(n))

  // Add new header and format data
  def format(name: String) = Flow[Double].scan(name)((_, n) => f"$n%1.2f")


  // Parse incoming CSV and append SMA
  def appendSma(n: Int): Flow[ByteString, ByteString, Unit] = Flow(
    parse,
    Broadcast[Array[String]](2),
    select("Adj Close"),
    calc(n),
    format(s"Adj Close SMA($n)"),
    ZipWith((row: Array[String], col: String) => row :+ col),
    csv
  )((_, _, _, _, _, _, mat) => mat) {
    implicit builder => (parse, bcast, select, calc, format, append, csv) =>

      parse ~> bcast                             ~> append.in0
               bcast ~> select ~> calc ~> format ~> append.in1
                                                    append.out ~> csv

      (parse.inlet, csv.outlet)
  }
}
