import http.HttpServer
import stock.StockPricesService
import bitcoin.BitcoinTradesService

import scala.util.Success

/**
 * Application that starts a server implementing two basic services.
 *
 * Notes:
 * - `HttpServer` is a custom trait that bootstraps Akka HTTP and starts a server to handle the service route(s).
 * - Services extend another custom trait, `HttpService`, that contains the service route. `HttpService` is
 *   designed to be stackable to allow a server to implement multiple services while promoting small, well-defined
 *   service routes.
 * - Default server and services configuration is contained in reference.conf in the resources directory.
 */
object Main extends App with HttpServer with StockPricesService with BitcoinTradesService {

  bindingFuture.andThen { case Success(binding) =>
    val host = binding.localAddress.getHostName
    val port = binding.localAddress.getPort
    println(
      s"""
         | Get started with the following URLs:
         | Stock Prices Service:
         |   Yahoo with default SMA        : http://$host:$port/stock/prices/daily/yhoo
         |   Yahoo 2 years w/ SMA(200)     : http://$host:$port/stock/prices/daily/yhoo?period=2y&calculated=sma(200)
         |   Facebook 1 year raw history   : http://$host:$port/stock/prices/daily/fb?period=1y&raw=true
         | Bitcoin Trades Service:
         |   Hourly OHLCV (Bitstamp USD)   : http://$host:$port/bitcoin/prices/hourly/bitstamp/USD
         |   Daily  OHLCV (itBit USD)      : http://$host:$port/bitcoin/prices/daily/itbit/USD
         |   Trades (itBit USD)            : http://$host:$port/bitcoin/trades/itbit/USD
         |   Trades Raw Response           : http://$host:$port/bitcoin/trades/bitstamp/USD?raw=true
      """.stripMargin.trim)
  }
}
