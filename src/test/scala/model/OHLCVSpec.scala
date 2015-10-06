package model

import java.time.Instant

import org.scalatest.{Matchers, WordSpec}

class OHLCVSpec extends WordSpec with Matchers {

  "OHLCV" should {

    "properly aggregate prepended trades" in {
      val now = Instant.now()
      val tuples = {
        Seq(
          (Trade(now, 100, 50), OHLCV(100, 100, 100, 100, 50, 1, now, now)),
          (Trade(now, 90, 50), OHLCV(90, 100, 90, 100, 100, 2, now, now)),
          (Trade(now.minusSeconds(1), 80, 50), OHLCV(80, 100, 80, 100, 150, 3, now.minusSeconds(1), now)),
          (Trade(now.minusSeconds(2), 110, 50), OHLCV(110, 110, 80, 100, 200, 4, now.minusSeconds(2), now)),
          (Trade(now.plusSeconds(3), 130, 50), OHLCV(110, 130, 80, 130, 250, 5, now.minusSeconds(2), now.plusSeconds(3)))
        )
      }

      tuples.scanLeft(Option.empty[OHLCV]) { case (prev, (trade, expected)) =>
          val cur = prev.map(trade +: _).orElse(Some(OHLCV(trade)))
          cur.get shouldBe expected
          cur
      }
    }

    "properly aggregate appended trades" in {
      val now = Instant.now()
      val tuples = {
        Seq(
          (Trade(now, 100, 50), OHLCV(100, 100, 100, 100, 50, 1, now, now)),
          (Trade(now, 90, 50), OHLCV(100, 100, 90, 90, 100, 2, now, now)),
          (Trade(now.plusSeconds(1), 80, 50), OHLCV(100, 100, 80, 80, 150, 3, now, now.plusSeconds(1))),
          (Trade(now.plusSeconds(2), 110, 50), OHLCV(100, 110, 80, 110, 200, 4, now, now.plusSeconds(2))),
          (Trade(now.minusSeconds(3), 130, 50), OHLCV(130, 130, 80, 110, 250, 5, now.minusSeconds(3), now.plusSeconds(2)))
        )
      }

      tuples.scanLeft(Option.empty[OHLCV]) { case (prev, (trade, expected)) =>
          val cur = prev.map(_ :+ trade).orElse(Some(OHLCV(trade)))
          cur.get shouldBe expected
          cur
      }
    }

  }

}
