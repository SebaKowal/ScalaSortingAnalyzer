package benchmark.full

import benchmark.pure.PureResult

/** Full benchmark result — extends PureResult with system-level metrics.
 *  Because it extends PureResult, the analysis page, exporter, and summary
 *  bar all work with both types without branching. */
case class FullResult(
                       // ── All PureResult fields ─────────────────────────────
                       pure: PureResult,
                       // ── System metrics (only populated when probe is enabled) ─
                       heapDeltaMb:   Double = 0.0,   // heap used delta across the run
                       allocRateMbS:  Double = 0.0,   // MB allocated per second
                       gcCollections: Long   = 0L,    // GC collections triggered
                       gcPauseMs:     Long   = 0L,    // total GC pause time ms
                       cpuTimeNs:     Long   = 0L,    // CPU thread time for the sort thread
                       cpuPercent:    Double = 0.0    // cpuTimeNs / wallTimeNs * 100
                     ):
  // ── Delegate all pure accessors ───────────────────────
  def algoName:             String  = pure.algoName
  def pattern:              String  = pure.pattern
  def size:                 Int     = pure.size
  def meanNs:               Long    = pure.meanNs
  def medianNs:             Long    = pure.medianNs
  def stdDevNs:             Double  = pure.stdDevNs
  def p90Ns:                Long    = pure.p90Ns
  def p95Ns:                Long    = pure.p95Ns
  def p99Ns:                Long    = pure.p99Ns
  def minNs:                Long    = pure.minNs
  def maxNs:                Long    = pure.maxNs
  def throughputElemsPerMs: Double  = pure.throughputElemsPerMs
  def comparisons:          Long    = pure.comparisons
  def swaps:                Long    = pure.swaps
  def writes:               Long    = pure.writes
  def isSorted:             Boolean = pure.isSorted
  def isStable:             Boolean = pure.isStable
  def hasFailure:           Boolean = pure.hasFailure
  def failureMsg:           String  = pure.failureMsg
  def timeMs:               Double  = pure.timeMs
  def timeMsStr:            String  = pure.timeMsStr// Empty file - no longer needed, using BenchmarkResult directly

