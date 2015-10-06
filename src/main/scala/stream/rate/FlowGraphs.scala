package stream.rate

import akka.actor.ActorSystem
import akka.stream.scaladsl.Flow

import scala.concurrent.duration.FiniteDuration

trait FlowGraphs {

  def constantDelay[T](delay: FiniteDuration)(implicit system: ActorSystem) =
    Flow[T].transform(() => DelayStage[T](Iterator.continually(delay)))

  def constantRate[T](rate: Rate)(implicit system: ActorSystem) =
    Flow[T].transform(() => DelayStage[T](Iterator.continually(rate.durationFor(1))))

  def poissonRate[T](rate: Rate)(implicit system: ActorSystem) =
    Flow[T].transform(() => DelayStage[T](Poisson.arrivalTimes(rate)))

}
