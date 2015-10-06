package stream.http

import akka.stream.io.InputStreamSource
import akka.stream.scaladsl._
import akka.util.ByteString

trait FlowGraphs {

  // Ensure a minimum chunk size to avoid over-chunking (small chunks)
  def chunkSize(minimumSize: Int = InputStreamSource.DefaultChunkSize) = Flow[ByteString]
    .transform(() => ChunkStage(minimumSize))

}
