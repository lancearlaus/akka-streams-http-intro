package support

import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.Suite

trait HttpServiceScalatest extends ScalatestRouteTest { this: Suite =>

  var config: Config = _
  var log: LoggingAdapter = _

  abstract override protected def beforeAll(): Unit = {
    super.beforeAll()
    config = ConfigFactory.load()
    log = Logging(system, getClass)
  }

  abstract override protected def afterAll(): Unit = {
    log = null
    config = null
    super.afterAll()
  }

}
