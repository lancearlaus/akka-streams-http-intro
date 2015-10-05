package config

import rate.Rate

import scala.concurrent.duration._
import scala.util.parsing.combinator.JavaTokenParsers


/**
 * Represents a named function call.
 *
 * @param name function name
 * @param args invocation arguments
 */
case class FunctionCall(name: String, args: List[Any])


/**
 * A simple function call parser combinator.
 *
 * Function calls are of the form `function(args)` where args is a (potentially empty) list of arguments.
 * Example: "myfunc(1, 2.0, true, 5 seconds)
 */
trait FunctionCallParser extends ArgumentsParser {
  lazy val call: Parser[FunctionCall] = ident ~ args ^^ { case name ~ args => FunctionCall(name, args) }
}

/**
 * A simple function arguments parser combinator.
 *
 * Parses an argument list and accepts Rate, Duration, Boolean, String, and Double expression arguments.
 */
trait ArgumentsParser extends RateParser {

  lazy val args: Parser[List[Any]] = "(" ~> repsep(arg, ",") <~ ")"

  lazy val arg: Parser[Any] = (rate | duration | expr | booleanLiteral | stringLiteral) ^^ {
    case t: Tree => eval(t)
    case x => x
  }

  lazy val booleanLiteral = ("true" | "false") ^^ { t => t.toBoolean }
}


/**
 * Parses a rate, which is a quantity over a finite duration.
 * Examples:
 * "1 / s"
 * "300 per second"
 * "1 every 3 seconds"
 *
 * Note: A word form of the quantity / duration separator must be used when specifying both a quantity and
 * duration length.
 * Example: "2 every 3 seconds", NOT "2 / 3 seconds"
 */
trait RateParser extends DurationParser {

  lazy val rate = expr ~ per ~ finiteDuration ^^ {
    case (quantity ~ per ~ duration) => Rate(eval(quantity), duration)
  }

  lazy val per = ("per" | "every" | "/")
}

/**
 * Parses durations with optional length such as "3 seconds" and "second".
 * Length defaults to 1 if not specified ("second" is equivalent to "1 second").
 */
trait DurationParser extends ExpressionParser {

  // Duration is an optional length followed by a time unit
  lazy val duration = opt(expr) ~ timeUnit ^^ { case length ~ unit =>
    Duration(eval(length.getOrElse(Num(1))), unit)
  }

  lazy val finiteDuration = duration ^? { case d: FiniteDuration => d }

  // Use Duration class to extract time unit
  lazy val timeUnit = ident ^^ ("1 " + _) ^? { case Duration(_, unit) => unit }
}


/**
 * A simple math expression parser combinator.
 *
 * Thanks: https://gist.github.com/sschaef/5529436
 */
trait ExpressionParser extends JavaTokenParsers {

  sealed abstract class Tree
  case class Add(t1: Tree, t2: Tree) extends Tree
  case class Sub(t1: Tree, t2: Tree) extends Tree
  case class Mul(t1: Tree, t2: Tree) extends Tree
  case class Div(t1: Tree, t2: Tree) extends Tree
  case class Num(t: Double) extends Tree

  def eval(t: Tree): Double = t match {
    case Add(t1, t2) => eval(t1)+eval(t2)
    case Sub(t1, t2) => eval(t1)-eval(t2)
    case Mul(t1, t2) => eval(t1)*eval(t2)
    case Div(t1, t2) => eval(t1)/eval(t2)
    case Num(t) => t
  }

  lazy val expr: Parser[Tree] = term ~ rep("[+-]".r ~ term) ^^ {
    case t ~ ts => ts.foldLeft(t) {
      case (t1, "+" ~ t2) => Add(t1, t2)
      case (t1, "-" ~ t2) => Sub(t1, t2)
    }
  }

  lazy val term = factor ~ rep("[*/]".r ~ factor) ^^ {
    case t ~ ts => ts.foldLeft(t) {
      case (t1, "*" ~ t2) => Mul(t1, t2)
      case (t1, "/" ~ t2) => Div(t1, t2)
    }
  }

  lazy val factor = "(" ~> expr <~ ")" | num

  lazy val num = floatingPointNumber ^^ { t => Num(t.toDouble) }
}


