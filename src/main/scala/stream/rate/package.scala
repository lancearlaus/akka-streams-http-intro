package stream

import scala.concurrent.duration.{Duration, FiniteDuration}

package object rate extends FlowGraphs {

  /**
   * Type class to support natural expression of rates and related calculations.
   *
   * Usage:
   * import scala.concurrent.duration._
   * import model.rate._
   *
   * val rate = 10 per 1.second
   * val duration = 10 / rate  // Result: 1 second
   */
  implicit class RateNumeric[T : Numeric](n: T) {
    // Support Rate expressed as ratio of number to duration e.g. 1 / sec or 1 per 300 millis
    def per(duration: FiniteDuration): Rate = Rate(implicitly[Numeric[T]].toDouble(n), duration)
    def every(duration: FiniteDuration) = per(duration)
    def /(duration: FiniteDuration) = per(duration)

    def /(rate: Rate): Duration = (implicitly[Numeric[T]].toDouble(n) * rate.quantity) * rate.duration
  }

}
