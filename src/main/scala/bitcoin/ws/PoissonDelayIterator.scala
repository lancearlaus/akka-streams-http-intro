package bitcoin.ws

/**
 * Generates a series of inter-event timings for a Poisson distribution.
 *
 * See: http://preshing.com/20111007/how-to-generate-random-timings-for-a-poisson-process/
 *
 * @param mean mean delay between events
 */
case class PoissonDelayIterator(mean: Double) extends Iterator[Double] {
  override def hasNext = true
  override def next() = -Math.log(1.0 - Math.random) * mean
}
