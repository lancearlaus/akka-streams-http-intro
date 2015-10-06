package stream.rate

import scala.concurrent.duration.FiniteDuration

object Poisson {

  /**
   * Calculates a random Poisson timing (arrival time) for the given mean arrival model.rate.
   *
   * Example: Generate arrival time for an event with a mean model.rate of 1 event per 250 msec.
   * Poisson.arrivalTime(1 / 250 millis)
   */
  def arrivalTime(rate: Rate): FiniteDuration =
    (-Math.log(1.0 - Math.random) * (rate.duration / rate.quantity)).asInstanceOf[FiniteDuration]

  def arrivalTimes(rate: Rate) = Iterator.continually(arrivalTime(rate))

}