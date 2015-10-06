import service.bitcoin.BitcoinTradesService
import service.bitcoin.ws.BitcoinTradesWebsocketService
import service.http.HttpServer
import service.stock.StockPriceService

import scala.util.Success

/**
 * Application that starts a server implementing two basic services.
 *
 * Notes:
 * - `HttpServer` is a custom trait that bootstraps Akka HTTP and starts a server to handle the service route(s).
 * - Services extend `HttpService`, a custom trait that defines the service route. `HttpService` is
 *   designed to be stackable to promote small, well-defined services while allowing a server to implement
 *   multiple services.
 * - Default server and services configuration is contained in reference.conf in the resources directory.
 */
object Main extends App
  with HttpServer
  with StockPriceService
  with BitcoinTradesService
  with BitcoinTradesWebsocketService
{
  bindingFuture.andThen { case Success(binding) =>
    val host = binding.localAddress.getHostName
    val port = binding.localAddress.getPort
    println(
      s"""
         | Get started with the following URLs:
         | Stock Price Service:
         |   Yahoo with default SMA        : http://$host:$port/stock/price/daily/yhoo
         |   Yahoo 2 years w/ SMA(200)     : http://$host:$port/stock/price/daily/yhoo?period=2y&calculated=sma(200)
         |   Facebook 1 year raw history   : http://$host:$port/stock/price/daily/fb?period=1y&raw=true
         | Bitcoin Trades Service:
         |   Hourly OHLCV (Bitstamp USD)   : http://$host:$port/bitcoin/price/hourly/bitstamp/USD
         |   Daily  OHLCV (itBit USD)      : http://$host:$port/bitcoin/price/daily/itbit/USD
         |   Recent trades (itBit USD)     : http://$host:$port/bitcoin/trades/itbit/USD
         |   Trades raw response           : http://$host:$port/bitcoin/trades/bitstamp/USD?raw=true
         | Bitcoin Random Trades Websocket Service:
         |   Periodic random trades        : ws://$host:$port/bitcoin/random/trades
      """.stripMargin.trim)
  }
}
