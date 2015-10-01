package bitcoin

import java.time._
import java.time.temporal.ChronoUnit._

import scala.math.Ordering.Implicits._
import scala.util.Try


// A half-open time interval [begin, end)
case class Interval(begin: Instant, end:Instant) {
  require(begin <= end)
  def contains(instant: Instant) = (begin <= instant) && (instant < end)
}
object Interval {
  val empty = Interval(Instant.MIN, Instant.MIN)
}


// Segments a time line into aligned periodic intervals
sealed trait Periodic {
  // Returns the periodic interval containing an Instant
  def interval(instant: Instant): Interval
}

object Periodic {

  val HourlyPattern = "(?i)hourly".r
  val DailyPattern = "(?i)daily".r
  val ZonedDailyPattern = "(?i)daily\\(([^\\)]+)\\)".r

  def unapply(s: String)(implicit defaultZoneId: ZoneId = Daily.DefaultZoneId): Option[Periodic] = s match {
    case HourlyPattern(_*)      => Some(Hourly)
    case DailyPattern(_*)       => Some(Daily(defaultZoneId))
    case ZonedDailyPattern(id)  => Try(ZoneId.of(id)).toOption.map(z => Daily(z))
    case _ => None
  }
}

case class Daily(zoneId: ZoneId) extends Periodic {
  override def interval(instant: Instant): Interval = {
    val truncated = instant.atZone(zoneId).truncatedTo(DAYS).toInstant
    Interval(truncated, truncated.plus(1, DAYS))
  }
}
object Daily {
  val DefaultZoneId = ZoneOffset.UTC
  // Segment time line using system default time zone
  def apply(): Daily = Daily(DefaultZoneId)
}

object Hourly extends Periodic {
  override def interval(instant: Instant): Interval = {
    val truncated = instant.truncatedTo(HOURS)
    Interval(truncated, truncated.plus(1, HOURS))
  }
}
