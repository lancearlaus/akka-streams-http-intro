import service.http.HttpServer
import service.stock.mock.MockStockPriceService

// Mock server
object Mock extends App
    with HttpServer
    with MockStockPriceService
{
  // Increment port to allow simultaneous real and mock servers
  override def port = super.port + 1
}
