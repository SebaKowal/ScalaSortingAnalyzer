package benchmark

/** Computes full latency distribution from a sequence of nanosecond samples. */
object LatencyStats:

  case class Stats(
                    mean:   Long,
                    median: Long,
                    p90:    Long,
                    p95:    Long,
                    p99:    Long,
                    min:    Long,
                    max:    Long,
                    stdDev: Double
                  )

  def compute(samples: Seq[Long]): Stats =
    if samples.isEmpty then
      return Stats(0, 0, 0, 0, 0, 0, 0, 0.0)

    val sorted = samples.sorted
    val n      = sorted.length
    val mean   = sorted.sum / n
    val stdDev = math.sqrt(sorted.map(x => math.pow((x - mean).toDouble, 2)).sum / n)

    def percentile(p: Double): Long =
      val idx = math.ceil(p / 100.0 * n).toInt - 1
      sorted(idx.max(0).min(n - 1))

    Stats(
      mean   = mean,
      median = percentile(50),
      p90    = percentile(90),
      p95    = percentile(95),
      p99    = percentile(99),
      min    = sorted.head,
      max    = sorted.last,
      stdDev = stdDev
    )