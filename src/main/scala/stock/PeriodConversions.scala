package stock

import java.time.Period
import java.time.format.DateTimeParseException

import akka.http.scaladsl.unmarshalling.Unmarshaller

object PeriodConversions {

  // Implicit conversion from String (with or without "P" prefix) to Period
  implicit def stringToPeriod(s: String): Period = s.head match {
    case 'P'|'p' => Period.parse(s)
    case _ => Period.parse("P" + s)
  }
  // Unmarshaller, for use in parameter directives
  // Usage (in a route): parameter('period.as[Period])
  implicit val periodFromStringUnmarshaller: Unmarshaller[String, Period] =
    Unmarshaller.strict[String, Period](s => stringToPeriod(s))

}
