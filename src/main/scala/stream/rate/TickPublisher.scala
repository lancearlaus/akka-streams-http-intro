package stream.rate

import akka.actor.{ActorLogging, Cancellable, Props}
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import akka.stream.scaladsl.Source
import com.typesafe.config.Config
import config.{FunctionCall, FunctionCallParser}
import service.bitcoin.ws.FlowGraphs.tick

import scala.concurrent.duration.FiniteDuration

/**
 * Emits elements with intervals specified by a series of durations.
 *
 * The duration between elements is determined by the supplied iterator, subject to downstream demand.
 * The purpose is to enable variable timing between elements to simulate real-world events that
 * may follow a statistical distribution (Poisson, for example).
 *
 * This source starts emitting in response to initial downstream demand and will continue to emit as long as
 * there is sufficient downstream demand.
 *
 * The `failOnOverflow` parameter determines what happens when downstream demand is exhausted i.e. if a tick arrives
 * and `totalDemand == 0`. If false (the default), ticks are paused pending downstream demand. The previous tick is
 * backlogged and emitted immediately upon additional demand. If `failOnOverflow` is true, the stream is failed, the
 * purpose of which is to support testing of downstream processing rates.
 *
 * The scheduling algorithm is intentionally simple for demonstration purposes and does not attempt to perform
 * adjustments to match actual with desired element timing/model.rate.
 *
 * Scheduling is done using the Akka scheduler, so any caveats about timing regarding that scheduler, notably
 * its granularity, are applicable here. The actual emission model.rate is, therefore, limited by the scheduler's granularity
 * (1 element per scheduler tick) and is likely to be different (less) than desired.
 *
 * @param durations desired inter-event timings
 * @param failOnOverflow fail the stream upon downstream demand exhaustion. If false (the default), ticks are paused
 *                       pending downstream demand
 */
class TickPublisher(durations: Iterator[FiniteDuration], failOnOverflow: Boolean = false)
    extends ActorPublisher[FiniteDuration]
    with ActorLogging
{
  import TickPublisher._

  var cancellable = Option.empty[Cancellable]
  var backlog = Option.empty[FiniteDuration]

  override def receive: Receive = {

    case msg @ Request(n) => {
      log.debug(s"Received $msg")
      if (cancellable.isEmpty) {
        backlog.map(d => onNext(d))
        backlog = None
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
      if (isActive) {
        if (totalDemand > 0) {
          onNext(duration)
          scheduleTick
        } else {
          if (failOnOverflow) onErrorThenStop(TickOverflowException("downstream demand exhausted"))
          else {
            log.warning(s"downstream demand exhausted, pausing ticks pending additional demand")
            backlog = Some(duration)
          }
        }
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

  final case class TickOverflowException(msg: String) extends RuntimeException(msg)

  object config {

    implicit class ConfigTickSource(config: Config) extends FunctionCallParser {

      /**
       * Creates a tick Source from configuration property.
       *
       * Two types of tick sources are currently supported.
       * Constant - Source that emits ticks at regular intervals (a constant model.rate)
       * Ex: service.service.bitcoin.trades.ticks: "constant(1 every 250 millis)"
       * Poisson - Source that emits ticks according a Poisson distribution with the given mean model.rate
       * Ex: service.service.bitcoin.trades.ticks: "poisson(100 per second)"
       *
       * @param path configuration property path from which to read configuration
       * @return cancellable tick source
       */
      def getTickSource(path: String): Source[FiniteDuration, Cancellable] = {
        val value = config.getString(path)
        parseAll(rate | call, value) match {
          case Success(rate @ Rate(_, _), _) => tick.constant(rate)
          case Success(FunctionCall("constant", List(rate: Rate)), _) => tick.constant(rate)
          case Success(FunctionCall("poisson", List(rate: Rate)), _) => tick.poisson(rate)
          case NoSuccess(msg, _) =>
            throw new Exception(s"failed to parse tick source configuration value (msg: $msg)")
        }
      }

    }
  }

}
