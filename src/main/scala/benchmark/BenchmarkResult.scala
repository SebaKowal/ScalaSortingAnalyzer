package benchmark

case class BenchmarkResult(
                            algoName:      String,
                            variant:       String,
                            pattern:       String,
                            size:          Int,
                            isWarm:        Boolean,
                            // Time — full distribution
                            timeNs:        Long,
                            timeMeanNs:    Long    = 0L,
                            timeMedianNs:  Long    = 0L,
                            timeP90Ns:     Long    = 0L,
                            timeP95Ns:     Long    = 0L,
                            timeP99Ns:     Long    = 0L,
                            timeMinNs:     Long    = 0L,
                            timeMaxNs:     Long    = 0L,
                            timeStdDevNs:  Double  = 0.0,
                            // Throughput
                            throughput:    Double  = 0.0,
                            opsPerSec:     Double  = 0.0,
                            // Algorithmic
                            comparisons:   Long    = 0L,
                            swaps:         Long    = 0L,
                            writes:        Long    = 0L,
                            // JVM memory
                            maxHeapMb:   Double  = 0.0,
                            allocRateMbS:  Double  = 0.0,
                            gcCollections: Long    = 0L,
                            gcPauseMs:     Long    = 0L,
                            // CPU
                            cpuTimeNs:     Long    = 0L,
                            userTimeNs:    Long    = 0L,
                            cpuPercent:    Double  = 0.0,
                            // Correctness
                            isSorted:      Boolean = false,
                            isStable:      Boolean = false,
                            // Failure info
                            failureMsg:    String  = ""
                          ):
  def timeMs: Double     = timeNs / 1_000_000.0
  def timeMsStr: String  = f"$timeMs%.2f ms"
  def throughputStr: String = f"$throughput%.0f el/ms"
  def allocStr: String   = f"$allocRateMbS%.2f MB/s"
  def hasFailure: Boolean = failureMsg.nonEmpty