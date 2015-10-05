package stage

import akka.stream.stage.{Context, PushPullStage, SyncDirective, TerminationDirective}
import akka.util.ByteString


/**
 * Stage that assembles small chunks into larger chunks to avoid over chunking
 * a HTTP response.
 *
 * @param minimumSize minimum size of each emitted chunk, aside from the last
 */
case class ChunkingStage(minimumSize: Int) extends PushPullStage[ByteString, ByteString] {
  private var buffer = ByteString()

  override def onPush(elem: ByteString, ctx: Context[ByteString]): SyncDirective = {
    val concat = buffer ++ elem

    if (concat.size >= minimumSize) {
      buffer = ByteString()
      ctx.push(concat)
    } else {
      buffer = concat
      ctx.pull()
    }
  }

  override def onPull(ctx: Context[ByteString]): SyncDirective = {
    if (!ctx.isFinishing) {
      ctx.pull()
    } else {
      if (!buffer.isEmpty) ctx.pushAndFinish(buffer)
      else ctx.finish()
    }
  }

  override def onUpstreamFinish(ctx: Context[ByteString]): TerminationDirective = {
    ctx.absorbTermination()
  }

}
