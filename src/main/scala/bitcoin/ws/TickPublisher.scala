package bitcoin.ws

import akka.actor.{Cancellable, Props}
import akka.event.Logging
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}

import scala.concurrent.duration.{FiniteDuration, _}

/**
 * Emits elements with intervals specified by a Duration Source.
 *
 * The duration between elements is determined by the supplied Duration iterator.
 * The purpose is to enable variable timing between elements to simulate real-world events that
 * may follow a statistical distribution (Poisson, for example).
 *
 * The scheduling algorithm is intentionally simple and does not attempt to perform adjustments to
 * match actual with desired element timing/rate.
 * Scheduling is done using the Akka scheduler, so any caveats about timing regarding that scheduler, notably
 * its granularity, are applicable here. The actual emission rate is likely to be different (less) than desired.
 *
 * @param durations desired inter-event timings
 */
class TickPublisher(durations: Iterator[FiniteDuration]) extends ActorPublisher[FiniteDuration] {
  import TickPublisher._

  val log = Logging(this)

  var cancellable = Option.empty[Cancellable]

  override def receive: Receive = {

    case msg @ Request(n) => {
      log.debug(s"Received $msg")
      if (cancellable.isEmpty) {
        scheduleTick
      }
    }

    case msg @ Cancel => {
      log.debug(s"Received $msg")
      cancelTick
    }

    case msg @ Tick(duration) => {
      log.debug(s"Received $msg")
      cancellable = None
      if (isActive && totalDemand > 0) {
        onNext(duration)
        scheduleTick
      }
    }

  }

  def cancelTick: Unit = {
    cancellable.map(_.cancel)
    cancellable = None
  }

  def scheduleTick: Unit = {
    if (isActive) {
      if (durations.hasNext) {
        val duration = durations.next

        log.debug(s"Scheduling tick with delay $duration")
        import context.dispatcher
        cancellable = Some(context.system.scheduler.scheduleOnce(duration, self, Tick(duration)))
      } else {
        onCompleteThenStop
      }
    }
  }

  override def postStop() = cancelTick

}

object TickPublisher {
  def props(durations: Iterator[FiniteDuration]) = Props(new TickPublisher(durations))

  private final case class Tick(duration: FiniteDuration)
}
