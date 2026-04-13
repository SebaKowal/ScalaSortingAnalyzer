package benchmark

import algorithms.AlgorithmRegistry
import model.{AlgorithmType, ArrayGenerator, GeneratorType, SortStep}

object BenchmarkRunner:

  private val WarmupRuns  = 500
  private val MeasureRuns = 10

  case class RunConfig(
                        algo:      AlgorithmType,
                        generator: GeneratorType,
                        size:      Int,
                        warmup:    Boolean
                      )

  def run(config: RunConfig, onProgress: String => Unit): Seq[BenchmarkResult] =
    val results = collection.mutable.ArrayBuffer.empty[BenchmarkResult]

    if config.warmup then
      onProgress(s"Warming up ${config.algo.label}…")
      for _ <- 0 until WarmupRuns do
        val arr = ArrayGenerator.generate(config.generator, config.size)
        AlgorithmRegistry.get(config.algo).steps(arr).foreach(_ => ())

    val coldArr = ArrayGenerator.generate(config.generator, config.size)
    results += measure(config.algo, config.generator, config.size, coldArr, isWarm = false)

    if config.warmup then
      onProgress(s"Measuring ${config.algo.label} (warm)…")
      results ++= (0 until MeasureRuns).map { _ =>
        val arr = ArrayGenerator.generate(config.generator, config.size)
        measure(config.algo, config.generator, config.size, arr, isWarm = true)
      }

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

    // ── Replay steps onto a working copy ─────────────────────
    // steps() clones internally, so arr is never mutated.
    // We must replay mutations ourselves to get the final sorted array.
    val working = arr.clone()

    AlgorithmRegistry.get(algo).steps(arr).foreach {
      case SortStep.Compare(_, _)    => comparisons += 1
      case SortStep.Swap(i, j)       =>
        swaps += 1
        val tmp = working(i); working(i) = working(j); working(j) = tmp
      case SortStep.Set(idx, value)  =>
        writes += 1
        working(idx) = value
      case _                         =>
    }

    val timeNs    = System.nanoTime() - t0
    val snapAfter = SystemMonitor.snapshot()

    val heapDelta  = (snapAfter.heapMb        - snapBefore.heapMb).max(0.0)
    val gcDelta    = (snapAfter.gcCollections  - snapBefore.gcCollections).max(0)
    val gcTime     = (snapAfter.gcTimeMs       - snapBefore.gcTimeMs).max(0)
    val timeMs     = timeNs / 1_000_000.0
    val allocRate  = if timeMs > 0 then heapDelta / (timeMs / 1000.0) else 0.0
    val throughput = if timeMs > 0 then size / timeMs else 0.0

    // ── Correctness: check the working copy, not the input ───
    val isSorted = isSortedAsc(working)

    // ── Sanity check: working must equal reference sort ───────
    val reference = arr.clone()
    java.util.Arrays.sort(reference)
    val isCorrect = java.util.Arrays.equals(working, reference)

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
      isSorted      = isSorted && isCorrect,
      isStable      = StabilityChecker.isAlgorithmStable(algo.label)
    )

  /** Robust ascending sort check — handles empty, single element, duplicates */
  private def isSortedAsc(arr: Array[Int]): Boolean =
    if arr.length <= 1 then return true
    var i = 1
    while i < arr.length do
      if arr(i) < arr(i - 1) then return false
      i += 1
    true