package benchmark.pure

import model.GeneratorType
import ui.utils.AlgorithmType

/** Immutable result from a pure benchmark run.
 *  Contains only timing distribution and algorithmic op counts.
 *  Extended by FullResult for system metrics.
 *
 *  Op counts are collected in a separate non-timed pass —
 *  they are never inside the nanoTime() window. */
case class PureResult(
                       // ── Identity ─────────────────────────────────────────────
                       algoName:    String,
                       pattern:     String,
                       size:        Int,

                       // ── Latency distribution (nanoseconds) ───────────────────
                       meanNs:      Long,
                       medianNs:    Long,
                       stdDevNs:    Double,
                       p90Ns:       Long,
                       p95Ns:       Long,
                       p99Ns:       Long,
                       minNs:       Long,
                       maxNs:       Long,

                       // ── Throughput ───────────────────────────────────────────
                       throughputElemsPerMs: Double,

                       // ── Algorithmic op counts (non-timed pass) ───────────────
                       comparisons: Long,
                       swaps:       Long,
                       writes:      Long,

                       // ── Correctness ──────────────────────────────────────────
                       isSorted:    Boolean,
                       isStable:    Boolean,

                       // ── Run metadata ─────────────────────────────────────────
                       warmupRounds:  Int,
                       measureRounds: Int,
                       failureMsg:    String = ""
                     ):
  def timeMs: Double     = meanNs / 1_000_000.0
  def timeMsStr: String  = f"$timeMs%.2f ms"
  def hasFailure: Boolean = failureMsg.nonEmpty