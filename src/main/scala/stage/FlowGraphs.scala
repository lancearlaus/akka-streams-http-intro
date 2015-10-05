package stage

import akka.stream.io.InputStreamSource
import akka.stream.scaladsl._
import akka.util.ByteString

trait FlowGraphs {

  object chunk {

    // Ensure a minimum chunk size to avoid over-chunking (small chunks)
    def min(minimumSize: Int = InputStreamSource.DefaultChunkSize) = Flow[ByteString]
      .transform(() => ChunkingStage(minimumSize))

  }

}
