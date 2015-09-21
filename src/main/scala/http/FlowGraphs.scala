package http

import akka.stream.scaladsl._
import akka.util.ByteString

trait FlowGraphs {

  object chunk {

    // Ensure a minimum chunk size to avoid over-chunking (small chunks)
    def min(minimumSize: Int) = Flow[ByteString]
      .transform(() => ChunkingStage(minimumSize))

  }

}
