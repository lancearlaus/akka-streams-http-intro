package csv

import akka.stream.io.Framing
import akka.stream.scaladsl.FlowGraph.Implicits._
import akka.stream.scaladsl._
import akka.util.ByteString

trait FlowGraphs {

  // A CSV file row, parsed into columns
  type Row = Array[String]

  // Parse incoming bytes into CSV record stream
  // Note: Each ByteString may contain more (or less) than one line
  def parse(maximumLineLength: Int = 256): Flow[ByteString, Row, Unit] =
    Framing.delimiter(ByteString("\n"), maximumLineLength, allowTruncation = true)
      .map(_.utf8String.split("\\s*,\\s*"))

  // Select a specific column (including header) by name
  def select(name: String): Flow[Row, String, Unit] = Flow[Row]
    .prefixAndTail(1).map { case (header, rows) =>
      header.head.indexOf(name) match {
        case -1    => Source.empty[String]    // Named column not found
        case index => Source.single(name).concatMat(rows.map(_(index)))(Keep.right)
      }
    }.flatten(FlattenStrategy.concat)

  // Convert Row into CSV formatted ByteString
  lazy val format = Flow[Row].map(row => ByteString(row.mkString("", ",", "\n")))

}
