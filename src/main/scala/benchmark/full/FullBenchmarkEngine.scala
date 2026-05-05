package benchmark.full

import algorithms.purebenchmark.PureAlgorithmRegistry
import benchmark.LatencyStats
import benchmark.pure.{PureBenchmarkEngine, PureResult}

/** Full benchmark engine — rich system metrics, timing accuracy is secondary.
 *
 *  Delegates all timing to PureBenchmarkEngine — does NOT re-implement
 *  the timing loop. Adds system probing (heap, GC, CPU) around each round.
 *
 *  Use PureBenchmarkEngine when timing accuracy is critical. */
object FullBenchmarkEngine:

  /** Run a full benchmark for the given config.
   *  Returns Left(errorMessage) if correctness validation fails,
   *  Right(FullResult) on success. */
  def run(config: FullRunConfig): Either[String, FullResult] =
    Snapshot.enableCpuTime()

    val sortFn = PureAlgorithmRegistry.get(config.algo)

    // ── 1. Delegate timing to PureBenchmarkEngine ─────────
    val pureResult: PureResult = PureBenchmarkEngine.run(config.toPureConfig) match
      case Left(err)     => return Left(err)
      case Right(result) => result

    // ── 2. System probing pass (separate from timing) ─────
    // Run additional rounds with system snapshots.
    // These are NOT used for timing — only for system metrics.
    val records = Steps.measureAll(
      sortFn    = sortFn,
      generator = config.generator,
      size      = config.size,
      rounds    = config.measureRounds,
      probeHeap = config.probeHeap,
      probeCpu  = config.probeCpu,
      probeGc   = config.probeGc
    )

    // ── 3. Aggregate system metrics across rounds ─────────
    val avgHeapDelta   = if config.probeHeap then avg(records.map(_.heapDeltaMb)) else 0.0
    val avgGcCollect   = if config.probeGc   then records.map(_.gcCollections).sum / records.length else 0L
    val avgGcPause     = if config.probeGc   then records.map(_.gcPauseMs).sum / records.length else 0L
    val avgCpuTimeNs   = if config.probeCpu  then records.map(_.cpuTimeNs).sum / records.length else 0L
    val avgWallNs      = records.map(_.wallNs).sum / records.length
    val allocRateMbS   = if config.probeHeap && avgWallNs > 0 then
      avgHeapDelta / (avgWallNs / 1_000_000_000.0)
    else 0.0
    val cpuPercent     = if config.probeCpu && avgWallNs > 0 then
      avgCpuTimeNs.toDouble / avgWallNs * 100.0
    else 0.0

    Right(FullResult(
      pure          = pureResult,
      heapDeltaMb   = avgHeapDelta,
      allocRateMbS  = allocRateMbS,
      gcCollections = avgGcCollect,
      gcPauseMs     = avgGcPause,
      cpuTimeNs     = avgCpuTimeNs,
      cpuPercent    = cpuPercent
    ))

  private def avg(xs: Array[Double]): Double =
    if xs.isEmpty then 0.0 else xs.sum / xs.length