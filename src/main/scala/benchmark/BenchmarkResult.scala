package benchmark

case class BenchmarkResult(
                            algoName:      String,
                            variant:       String,   // "Imperative" now, "Parallel"/"Functional" later
                            pattern:       String,
                            size:          Int,
                            isWarm:        Boolean,
                            // Time
                            timeNs:        Long,
                            throughput:    Double,   // elements / ms
                            // Algorithmic
                            comparisons:   Long,
                            swaps:         Long,
                            writes:        Long,
                            // JVM
                            heapDeltaMb:   Double,
                            allocRateMbS:  Double,   // MB/s allocated during sort
                            gcCollections: Long,
                            gcPauseMs:     Long,
                            // Correctness
                            isSorted:      Boolean,
                            isStable:      Boolean
                          ):
  def timeMs: Double = timeNs / 1_000_000.0
  def timeMsStr: String = f"$timeMs%.2f ms"
  def throughputStr: String = f"$throughput%.0f el/ms"
  def allocStr: String = f"$allocRateMbS%.2f MB/s"