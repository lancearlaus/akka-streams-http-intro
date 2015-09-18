package exchange

import java.time.Instant
import java.util.UUID

// See: https://docs.exchange.coinbase.com/#websocket-feed

trait Message {
  val sequence: Long
  val productId: String
  val time: Instant
}

sealed trait Side
case object Buy extends Side
case object Sell extends Side

case class MatchMessage(
    sequence: Long,
    productId: String,
    time:Instant,
    tradeId: Long,
    size: Double,
    price: Double,
    side: Side,
    makerOrderId: UUID,
    takerOrderId: UUID)
  extends Message

