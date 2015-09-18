package trades

import java.time.Instant
import java.time.temporal.ChronoUnit

import scala.math.Ordering.Implicits._


// A half-open time interval [begin, end)
case class Interval(begin: Instant, end:Instant) {
  def contains(instant: Instant) = (begin <= instant) && (instant < end)
}
object Interval {
  val empty = Interval(Instant.MIN, Instant.MIN)
}


// Segments a time line into periodic intervals
sealed trait Period {
  def interval(instant: Instant): Interval
}

object Period {
  // Calculates the periodic interval containing an instant
  def interval(instant: Instant, chronoUnit: ChronoUnit) = {
    val begin = instant.truncatedTo(chronoUnit)
    val end = begin.plus(1, chronoUnit)
    Interval(begin, end)
  }
}

case object Daily extends Period {
  override def interval(instant: Instant) =
    Period.interval(instant, ChronoUnit.DAYS)
}
case object Hourly extends Period {
  override def interval(instant: Instant) =
    Period.interval(instant, ChronoUnit.HOURS)
}
