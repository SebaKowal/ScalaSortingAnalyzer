package benchmark

import algorithms.AlgorithmRegistry
import model.{AlgorithmType, ArrayGenerator, GeneratorType, SortStep}

object BenchmarkRunner:

  private val WarmupRuns  = 500
  private val MeasureRuns = 20   // more samples = better distribution

  case class RunConfig(
                        algo:      AlgorithmType,
                        generator: GeneratorType,
                        size:      Int,
                        warmup:    Boolean
                      )

  def run(config: RunConfig, onProgress: String => Unit): Seq[BenchmarkResult] =
    CpuMonitor.enableCpuTime()
    val results = collection.mutable.ArrayBuffer.empty[BenchmarkResult]

    // ── Pre-flight correctness check ──────────────────────────
    val sampleArr = ArrayGenerator.generate(config.generator, config.size)
    val check     = CorrectnessValidator.validate(config.algo, config.generator.label, sampleArr)
    if !check.passed then
      // Return a single failure result — do not benchmark incorrect algorithms
      return Seq(BenchmarkResult(
        algoName    = config.algo.label,
        variant     = "Imperative",
        pattern     = config.generator.label,
        size        = config.size,
        isWarm      = false,
        timeNs      = 0L,
        isStable    = StabilityChecker.isAlgorithmStable(config.algo),
        failureMsg  = check.message
      ))

    // ── Warmup phase ──────────────────────────────────────────
    if config.warmup then
      onProgress(s"Warming up ${config.algo.label} ($WarmupRuns runs)…")
      for _ <- 0 until WarmupRuns do
        val arr = ArrayGenerator.generate(config.generator, config.size)
        AlgorithmRegistry.get(config.algo).steps(arr).foreach(_ => ())
      System.gc() // encourage GC before measurement

    // ── Cold run ──────────────────────────────────────────────
    val coldArr = ArrayGenerator.generate(config.generator, config.size)
    results += measureSingle(config.algo, config.generator, config.size, coldArr, isWarm = false)

    // ── Warm runs — collect full distribution ─────────────────
    if config.warmup then
      onProgress(s"Measuring ${config.algo.label} ($MeasureRuns warm runs)…")
      val samples = (0 until MeasureRuns).map { _ =>
        val arr = ArrayGenerator.generate(config.generator, config.size)
        measureSingle(config.algo, config.generator, config.size, arr, isWarm = true)
      }

      // Build aggregate warm result with full distribution
      val times    = samples.map(_.timeNs)
      val stats    = LatencyStats.compute(times)
      val avgComps = samples.map(_.comparisons).sum / samples.size
      val avgSwaps = samples.map(_.swaps).sum / samples.size
      val avgWrites= samples.map(_.writes).sum / samples.size

      // Calculate max heap globally for the aggregate and properly average the allocation delta
      val maxHeapAggregate     = samples.map(_.maxHeapMb).max
      val avgHeapDeltaForAlloc = samples.map(s => s.allocRateMbS * (s.timeNs / 1_000_000_000.0)).sum / samples.size

      val avgGcC   = samples.map(_.gcCollections).sum / samples.size
      val avgGcT   = samples.map(_.gcPauseMs).sum / samples.size
      val avgCpu   = samples.map(_.cpuTimeNs).sum / samples.size
      val avgUser  = samples.map(_.userTimeNs).sum / samples.size
      val timeMs   = stats.mean / 1_000_000.0

      val aggregate = BenchmarkResult(
        algoName      = config.algo.label,
        variant       = "Imperative",
        pattern       = config.generator.label,
        size          = config.size,
        isWarm        = true,
        timeNs        = stats.mean,
        timeMeanNs    = stats.mean,
        timeMedianNs  = stats.median,
        timeP90Ns     = stats.p90,
        timeP95Ns     = stats.p95,
        timeP99Ns     = stats.p99,
        timeMinNs     = stats.min,
        timeMaxNs     = stats.max,
        timeStdDevNs  = stats.stdDev,
        throughput    = if timeMs > 0 then config.size / timeMs else 0.0,
        opsPerSec     = if timeMs > 0 then 1000.0 / timeMs else 0.0,
        comparisons   = avgComps,
        swaps         = avgSwaps,
        writes        = avgWrites,
        maxHeapMb     = maxHeapAggregate,
        allocRateMbS  = if timeMs > 0 then avgHeapDeltaForAlloc / (timeMs / 1000.0) else 0.0,
        gcCollections = avgGcC,
        gcPauseMs     = avgGcT,
        cpuTimeNs     = avgCpu,
        userTimeNs    = avgUser,
        cpuPercent    = if stats.mean > 0 then avgCpu.toDouble / stats.mean * 100 else 0.0,
        isSorted      = samples.forall(_.isSorted),
        isStable      = StabilityChecker.isAlgorithmStable(config.algo)
      )

      // Add individual warm samples for distribution charts
      results ++= samples
      results += aggregate

    results.toSeq

  private def measureSingle(
                             algo:    AlgorithmType,
                             gen:     GeneratorType,
                             size:    Int,
                             arr:     Array[Int],
                             isWarm:  Boolean
                           ): BenchmarkResult =
    val working    = arr.clone()
    val snapBefore = SystemMonitor.snapshot()
    val (cpuBefore, userBefore) = CpuMonitor.currentThreadTimes()
    val t0         = System.nanoTime()

    var comparisons = 0L
    var swaps       = 0L
    var writes      = 0L

    AlgorithmRegistry.get(algo).steps(arr).foreach {
      case SortStep.Compare(_, _)   => comparisons += 1
      case SortStep.Swap(i, j)      =>
        swaps += 1
        val tmp = working(i); working(i) = working(j); working(j) = tmp
      case SortStep.Set(idx, value) =>
        writes += 1
        working(idx) = value
      case _ =>
    }

    val timeNs     = System.nanoTime() - t0
    val (cpuAfter, userAfter) = CpuMonitor.currentThreadTimes()
    val snapAfter  = SystemMonitor.snapshot()

    val heapDelta  = (snapAfter.heapMb        - snapBefore.heapMb).max(0.0)
    val maxHeap    = math.max(snapBefore.heapMb, snapAfter.heapMb)
    val gcDelta    = (snapAfter.gcCollections - snapBefore.gcCollections).max(0)
    val gcTime     = (snapAfter.gcTimeMs      - snapBefore.gcTimeMs).max(0)
    val cpuDelta   = (cpuAfter  - cpuBefore).max(0)
    val userDelta  = (userAfter - userBefore).max(0)
    val timeMs     = timeNs / 1_000_000.0

    // Correctness — check working copy against reference
    val reference  = arr.clone()
    java.util.Arrays.sort(reference)
    val isSorted   = java.util.Arrays.equals(working, reference) && checkSortedAsc(working)

    BenchmarkResult(
      algoName      = algo.label,
      variant       = "Imperative",
      pattern       = gen.label,
      size          = size,
      isWarm        = isWarm,
      timeNs        = timeNs,
      timeMeanNs    = timeNs,
      timeMedianNs  = timeNs,
      timeMinNs     = timeNs,
      timeMaxNs     = timeNs,
      throughput    = if timeMs > 0 then size / timeMs else 0.0,
      opsPerSec     = if timeMs > 0 then 1000.0 / timeMs else 0.0,
      comparisons   = comparisons,
      swaps         = swaps,
      writes        = writes,
      maxHeapMb     = maxHeap,
      allocRateMbS  = if timeMs > 0 then heapDelta / (timeMs / 1000.0) else 0.0,
      gcCollections = gcDelta,
      gcPauseMs     = gcTime,
      cpuTimeNs     = cpuDelta,
      userTimeNs    = userDelta,
      cpuPercent    = if timeNs > 0 then cpuDelta.toDouble / timeNs * 100 else 0.0,
      isSorted      = isSorted,
      isStable      = StabilityChecker.isAlgorithmStable(algo)
    )

  private def checkSortedAsc(arr: Array[Int]): Boolean =
    if arr.length <= 1 then return true
    var i = 1
    while i < arr.length do
      if arr(i) < arr(i - 1) then return false
      i += 1
    true