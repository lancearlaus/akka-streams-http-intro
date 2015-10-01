package bitcoin

import java.time._

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
      val duration = Duration.between(interval.begin, interval.end)

      interval.begin shouldBe instant
      duration shouldBe Duration.ofDays(1)
    }

  }

}
