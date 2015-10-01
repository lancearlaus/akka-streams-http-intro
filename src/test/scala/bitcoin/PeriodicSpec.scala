package bitcoin

import java.time._
import java.time.temporal.ChronoUnit
import scala.collection.JavaConverters._

import org.scalatest.{Matchers, WordSpec}

class IntervalSpec extends WordSpec with Matchers {

  "Interval" should {

    "treat equal begin and end as empty" in {
      val now = Instant.now()
      Interval(now, now).contains(now) shouldBe false
    }

    "implement closed beginning and open end" in {
      val now = Instant.now()
      Interval(now, now.plusMillis(1)).contains(now) shouldBe true
      Interval(now.minusMillis(1), now).contains(now) shouldBe false
    }

  }
}

class PeriodicSpec extends WordSpec with Matchers {

  "Daily" should {

    "split timeline at day boundary" in {
      val zoneId = ZoneId.systemDefault
      val midnight = ZonedDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT, zoneId)
      val instant = midnight.toInstant()
      val interval = Daily(zoneId).interval(instant)
      val preMidnight = midnight.minus(1, ChronoUnit.MILLIS)
      val preInterval = Daily(zoneId).interval(preMidnight.toInstant)
      val duration = Duration.between(interval.begin, interval.end)

      interval.begin shouldBe instant
      duration shouldBe Duration.ofDays(1)
      preInterval.end shouldBe interval.begin
    }

    "default to UTC" in {
      Daily().zoneId shouldBe ZoneOffset.UTC
    }
  }

  "Hourly" should {

    "split time at hour boundary" in {
      val now = Instant.now()
      val truncated = now.truncatedTo(ChronoUnit.HOURS)
      val thisHour = Hourly.interval(now)
      val prevHour = Hourly.interval(now.minus(1, ChronoUnit.HOURS))
      val nextHour = Hourly.interval(now.plus(1, ChronoUnit.HOURS))
      val duration = Duration.between(thisHour.begin, thisHour.end)

      duration shouldBe Duration.ofHours(1)
      thisHour.begin shouldBe truncated
      prevHour.end shouldBe thisHour.begin
      thisHour.end shouldBe nextHour.begin
    }
  }

  "Extractor" should {

    "extract Hourly" in {
      "hourly" match {
        case Periodic(Hourly) =>
      }
      "Hourly" match {
        case Periodic(Hourly) =>
      }
    }

    "extract Daily with default ZoneId" in {
      "daily" match {
        case Periodic(Daily(z)) => z shouldBe Daily.DefaultZoneId
      }
      "Daily" match {
        case Periodic(Daily(z)) => z shouldBe Daily.DefaultZoneId
      }
    }
    "extract Daily with explicit ZoneId" in {
      ZoneId.getAvailableZoneIds.asScala.map(s => ZoneId.of(s)).foreach { zoneId =>
        s"daily($zoneId)" match {
          case Periodic(Daily(z)) => z shouldBe zoneId
        }
      }
    }
    "use implicit ZoneId when extracting Daily without explicit ZoneId" in {
      ZoneId.getAvailableZoneIds.asScala.map(s => ZoneId.of(s)).foreach { implicit zoneId =>
        "daily" match {
          case Periodic(Daily(z)) => z shouldBe zoneId
        }
      }
    }

  }

}
