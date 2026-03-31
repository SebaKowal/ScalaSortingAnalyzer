package benchmark

import algorithms.AlgorithmRegistry
import model.{AlgorithmType, ArrayGenerator, GeneratorType, SortStep}

object BenchmarkRunner:

  private val WarmupRuns  = 500   // throwaway runs to trigger JIT
  private val MeasureRuns = 10    // averaged measurement runs

  case class RunConfig(
                        algo:      AlgorithmType,
                        generator: GeneratorType,
                        size:      Int,
                        warmup:    Boolean   // if true, do warmup first
                      )

  def run(config: RunConfig, onProgress: String => Unit): Seq[BenchmarkResult] =
    val results = collection.mutable.ArrayBuffer.empty[BenchmarkResult]

    // ── Warmup phase ──────────────────────────────────────────
    if config.warmup then
      onProgress(s"Warming up ${config.algo.label}…")
      for _ <- 0 until WarmupRuns do
        val arr = ArrayGenerator.generate(config.generator, config.size)
        AlgorithmRegistry.get(config.algo).steps(arr).foreach(_ => ())

    // ── Cold run (before warmup if warmup=false, or first run) ─
    val coldArr = ArrayGenerator.generate(config.generator, config.size)
    results += measure(config.algo, config.generator, config.size, coldArr, isWarm = false)

    // ── Warm runs ─────────────────────────────────────────────
    if config.warmup then
      onProgress(s"Measuring ${config.algo.label} (warm)…")
      val warmMeasurements = (0 until MeasureRuns).map { _ =>
        val arr = ArrayGenerator.generate(config.generator, config.size)
        measure(config.algo, config.generator, config.size, arr, isWarm = true)
      }
      // Add all warm runs
      results ++= warmMeasurements

    results.toSeq

  private def measure(
                       algo:      AlgorithmType,
                       generator: GeneratorType,
                       size:      Int,
                       arr:       Array[Int],
                       isWarm:    Boolean
                     ): BenchmarkResult =
    val snapBefore = SystemMonitor.snapshot()
    val t0         = System.nanoTime()

    var comparisons = 0L
    var swaps       = 0L
    var writes      = 0L

    AlgorithmRegistry.get(algo).steps(arr).foreach {
      case SortStep.Compare(_, _) => comparisons += 1
      case SortStep.Swap(_, _)    => swaps       += 1
      case SortStep.Set(_, _)     => writes      += 1
      case _                      =>
    }

    val timeNs     = System.nanoTime() - t0
    val snapAfter  = SystemMonitor.snapshot()

    val heapDelta  = (snapAfter.heapMb  - snapBefore.heapMb).max(0.0)
    val gcDelta    = (snapAfter.gcCollections - snapBefore.gcCollections).max(0)
    val gcTime     = (snapAfter.gcTimeMs      - snapBefore.gcTimeMs).max(0)
    val timeMs     = timeNs / 1_000_000.0
    val allocRate  = if timeMs > 0 then heapDelta / (timeMs / 1000.0) else 0.0
    val throughput = if timeMs > 0 then size / timeMs else 0.0

    // Correctness check — verify the result is actually sorted
    val sorted = arr.toSeq.sorted
    val isSorted = arr.toSeq == sorted

    BenchmarkResult(
      algoName      = algo.label,
      variant       = "Imperative",
      pattern       = generator.label,
      size          = size,
      isWarm        = isWarm,
      timeNs        = timeNs,
      throughput    = throughput,
      comparisons   = comparisons,
      swaps         = swaps,
      writes        = writes,
      heapDeltaMb   = heapDelta,
      allocRateMbS  = allocRate,
      gcCollections = gcDelta,
      gcPauseMs     = gcTime,
      isSorted      = isSorted,
      isStable      = StabilityChecker.isAlgorithmStable(algo.label)
    )