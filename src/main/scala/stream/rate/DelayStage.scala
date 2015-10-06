package stream.rate

import akka.actor.{ActorSystem, Cancellable}
import akka.stream.stage._

import scala.concurrent.duration.FiniteDuration

/**
 * Emits elements with intervals specified by a Duration Source.
 *
 * The duration between elements is determined by the supplied Duration iterator.
 * The purpose is to enable variable timing between elements to simulate real-world events that
 * may follow a statistical distribution (Poisson, for example).
 *
 * The scheduling algorithm is intentionally simple and does not attempt to perform adjustments to
 * match actual with desired element timing/model.rate.
 * Scheduling is done using the Akka scheduler, so any caveats about timing regarding that scheduler, notably
 * its granularity, are applicable here. The actual emission model.rate is likely to be different (less) than desired.
 *
 * @param delays desired inter-event timings
 */
case class DelayStage[T](delays: Iterator[FiniteDuration])(implicit system: ActorSystem)
    extends AsyncStage[T, T, T]
{
  var delay: FiniteDuration = _
  var cancellable = Option.empty[Cancellable]

  override def onAsyncInput(event: T, ctx: AsyncContext[T, T]): Directive = {
    cancellable = None
    if (ctx.isFinishing) ctx.pushAndFinish(event)
    else ctx.pushAndPull(event)
  }

  override def onPush(elem: T, ctx: AsyncContext[T, T]): UpstreamDirective = {
    if (delays.hasNext) {
      val delay = delays.next()
      val callback = ctx.getAsyncCallback()

      // Schedule element to be emitted after delay
      import system.dispatcher
      cancellable = Some(system.scheduler.scheduleOnce(delay){ callback.invoke(elem) })

      ctx.holdUpstream()
    } else {
      ctx.finish()
    }
  }

  override def onPull(ctx: AsyncContext[T, T]): DownstreamDirective = ctx.holdDownstream()

  override def onUpstreamFinish(ctx: AsyncContext[T, T]): TerminationDirective =
    cancellable.map(_ => ctx.absorbTermination).getOrElse(super.onUpstreamFinish(ctx))

  override def onUpstreamFailure(cause: Throwable, ctx: AsyncContext[T, T]): TerminationDirective = {
    cancellable.map(_.cancel)
    cancellable = None
    super.onUpstreamFailure(cause, ctx)
  }
}

