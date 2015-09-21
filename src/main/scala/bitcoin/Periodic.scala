package bitcoin

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


// Segments a time line into aligned periodic intervals
sealed trait Periodic {
  // Returns the periodic interval containing an instant
  def interval(instant: Instant): Interval
}

object Periodic {
  // Calculates the periodic interval containing an instant
  // for standard chronological units
  def interval(instant: Instant, chronoUnit: ChronoUnit): Interval = {
    val begin = instant.truncatedTo(chronoUnit)
    val end = begin.plus(1, chronoUnit)
    Interval(begin, end)
  }

  def unapply(s: String): Option[Periodic] = s match {
    case s if s matches "(?i)daily" => Some(Daily)
    case s if s matches "(?i)hourly" => Some(Hourly)
    case _ => None
  }
}

case object Daily extends Periodic {
  override def interval(instant: Instant) =
    Periodic.interval(instant, ChronoUnit.DAYS)
}
case object Hourly extends Periodic {
  override def interval(instant: Instant) =
    Periodic.interval(instant, ChronoUnit.HOURS)
}
