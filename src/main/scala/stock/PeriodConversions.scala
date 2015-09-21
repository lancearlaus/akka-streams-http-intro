package stock

import java.time.Period
import java.time.format.DateTimeParseException

import akka.http.scaladsl.unmarshalling.Unmarshaller

object PeriodConversions {

  implicit def stringToPeriod(s: String): Period = s.head match {
    case 'P'|'p' => Period.parse(s)
    case _ => Period.parse("P" + s)
  }
  // Unmarshaller, for use in parameter directives
  // Usage (in a route): parameter('period.as[Period])
  implicit val periodFromStringUnmarshaller: Unmarshaller[String, Period] =
    Unmarshaller.strict[String, Period](s => stringToPeriod(s))

  // Period extractor helper object
//  object JavaPeriod {
//    // Extract Period from String, with or without P prefix
//    def unapply(string: String): Option[Period] = {
//      try {
//        Some(stringToPeriod(string))
//      } catch {
//        case e: DateTimeParseException => None
//      }
//    }
//  }

}
