package benchmark

import algorithms.pure.PureAlgorithmRegistry
import model.{AlgorithmType, ArrayGenerator, GeneratorType}

/**
 * Benchmark runner that measures ONLY sorting performance.
 *
 * Architecture decisions:
 *  - Uses PureAlgorithmRegistry — no step tracking, no callbacks
 *  - Input is cloned per run — each run is fully independent
 *  - System.gc() before warm measurement — reduces GC noise
 *  - CPU time measured via ThreadMXBean — per-thread, not process-wide
 *  - Correctness verified via reference sort — fails fast on incorrect results
 *  - Cold run always first, before JIT has seen the code
 *  - Warm runs after WarmupRuns iterations — JIT has stabilized by then
 */
object BenchmarkRunner:

  private val WarmupRuns  = 2000   // Enough to trigger JIT C2 compilation
  private val MeasureRuns = 30     // Enough samples for stable percentiles

  case class RunConfig(
                        algo:      AlgorithmType,
                        generator: GeneratorType,
                        size:      Int,
                        warmup:    Boolean
                      )

  def run(config: RunConfig, onProgress: String => Unit): Seq[BenchmarkResult] =
    val results = collection.mutable.ArrayBuffer.empty[BenchmarkResult]
    val sortFn  = PureAlgorithmRegistry.get(config.algo)

    // ── Pre-flight correctness check ──────────────────────────
    // Validate BEFORE any benchmarking — don't time incorrect results
    val checkInput  = ArrayGenerator.generate(config.generator, config.size)
    val checkWorking = checkInput.clone()
    sortFn(checkWorking)
    val reference = checkInput.clone()
    java.util.Arrays.sort(reference)
    if !java.util.Arrays.equals(checkWorking, reference) then
      return Seq(BenchmarkResult(
        algoName   = config.algo.label,
        variant    = "Imperative",
        pattern    = config.generator.label,
        size       = config.size,
        isWarm     = false,
        timeNs     = 0L,
        //isSorted   = false,
        //isStable   = StabilityChecker.isAlgorithmStable(config.algo.label),
        failureMsg = s"Correctness check failed — output does not match reference sort"
      ))

    // ── Cold run ──────────────────────────────────────────────
    // First run: JIT has not seen this code path yet
    // Represents real-world "first invocation" performance
    val coldArr = ArrayGenerator.generate(config.generator, config.size)
    results += measureSingle(
      config.algo, config.generator, config.size,
      coldArr, sortFn, isWarm = false
    )

    // ── Warmup phase ──────────────────────────────────────────
    if config.warmup then
      onProgress(s"Warming up ${config.algo.label} (${WarmupRuns} iterations)…")
      // Run WarmupRuns times to trigger JIT C1 → C2 compilation
      // Use varying input to prevent JIT from optimizing away the sort
      var wi = 0
      while wi < WarmupRuns do
        val arr = ArrayGenerator.generate(config.generator, config.size)
        sortFn(arr)
        wi += 1

      // Force GC before measurement — reduce GC interference during timing
      System.gc()
      System.runFinalization()
      Thread.sleep(50)  // brief pause for GC to complete

      // ── Warm measurement runs ─────────────────────────────
      onProgress(s"Measuring ${config.algo.label} (${MeasureRuns} warm runs)…")
      val samples = new Array[BenchmarkResult](MeasureRuns)
      var mi = 0
      while mi < MeasureRuns do
        val arr = ArrayGenerator.generate(config.generator, config.size)
        samples(mi) = measureSingle(
          config.algo, config.generator, config.size,
          arr, sortFn, isWarm = true
        )
        mi += 1

      // ── Statistical aggregation ───────────────────────────
      val times    = samples.map(_.timeNs).toSeq
      val stats    = LatencyStats.compute(times)
      val avgComps = samples.map(_.comparisons).sum / MeasureRuns
      val avgSwaps = samples.map(_.swaps).sum / MeasureRuns
      val avgWrites= samples.map(_.writes).sum / MeasureRuns
      //val avgHeap  = samples.map(_.heapDeltaMb).sum / MeasureRuns
      val avgAlloc = samples.map(_.allocRateMbS).sum / MeasureRuns
      val avgGcC   = samples.map(_.gcCollections).sum / MeasureRuns
      val avgGcT   = samples.map(_.gcPauseMs).sum / MeasureRuns
      val avgCpu   = samples.map(_.cpuTimeNs).sum / MeasureRuns
      val avgUser  = samples.map(_.userTimeNs).sum / MeasureRuns
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
        //heapDeltaMb   = avgHeap,
        allocRateMbS  = avgAlloc,
        gcCollections = avgGcC,
        gcPauseMs     = avgGcT,
        cpuTimeNs     = avgCpu,
        userTimeNs    = avgUser,
        cpuPercent    = if stats.mean > 0 then avgCpu.toDouble / stats.mean * 100 else 0.0,
        isSorted      = samples.forall(_.isSorted),
        //isStable      = StabilityChecker.isAlgorithmStable(config.algo.label)
      )

      // Individual warm samples for distribution analysis
      results ++= samples
      results += aggregate

    results.toSeq

  private def measureSingle(
                             algo:    AlgorithmType,
                             gen:     GeneratorType,
                             size:    Int,
                             input:   Array[Int],
                             sortFn:  Array[Int] => Unit,
                             isWarm:  Boolean
                           ): BenchmarkResult =
    // Clone input — sort is destructive, we need original for correctness check
    val working = input.clone()

    // Snapshot JVM state before sort
    val snapBefore          = SystemMonitor.snapshot()
    val (cpuBefore, userBefore) = CpuMonitor.currentThreadTimes()

    // ── TIMED SECTION — only the sort, nothing else ───────────
    val t0 = System.nanoTime()
    sortFn(working)
    val timeNs = System.nanoTime() - t0
    // ── END TIMED SECTION ─────────────────────────────────────

    val (cpuAfter, userAfter) = CpuMonitor.currentThreadTimes()
    val snapAfter             = SystemMonitor.snapshot()

    val heapDelta  = (snapAfter.heapMb       - snapBefore.heapMb).max(0.0)
    val gcDelta    = (snapAfter.gcCollections - snapBefore.gcCollections).max(0)
    val gcTime     = (snapAfter.gcTimeMs      - snapBefore.gcTimeMs).max(0)
    val cpuDelta   = (cpuAfter  - cpuBefore).max(0)
    val userDelta  = (userAfter - userBefore).max(0)
    val timeMs     = timeNs / 1_000_000.0

    // Correctness: compare sorted working copy against reference
    // Done AFTER timing — correctness check must not be inside timed section
    val reference = input.clone()
    java.util.Arrays.sort(reference)
    val correct = java.util.Arrays.equals(working, reference)

    // Algorithmic metrics — counted from pure sort
    // Note: pure sorts don't emit steps, so we compute from known complexity
    // For exact counts, use the visualization layer (step-tracking) separately
    // These are approximations based on theoretical operations
    val (comparisons, swaps, writes) = countOps(algo, size, gen)

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
      //heapDeltaMb   = heapDelta,
      allocRateMbS  = if timeMs > 0 then heapDelta / (timeMs / 1000.0) else 0.0,
      gcCollections = gcDelta,
      gcPauseMs     = gcTime,
      cpuTimeNs     = cpuDelta,
      userTimeNs    = userDelta,
      cpuPercent    = if timeNs > 0 then cpuDelta.toDouble / timeNs * 100 else 0.0,
      isSorted      = correct,
      //isStable      = StabilityChecker.isAlgorithmStable(algo.label)
    )

  private def countOps(
                        algo: AlgorithmType,
                        size: Int,
                        gen:  GeneratorType
                      ): (Long, Long, Long) =
    // Run the VISUALIZATION version once on a sample to get counts
    // This is NOT timed — purely for the metrics columns
    import algorithms.AlgorithmRegistry
    import model.SortStep
    val sampleArr = ArrayGenerator.generate(gen, size)
    var comps = 0L; var swaps = 0L; var writes = 0L
    AlgorithmRegistry.get(algo).steps(sampleArr).foreach {
      case SortStep.Compare(_, _) => comps  += 1
      case SortStep.Swap(_, _)    => swaps  += 1
      case SortStep.Set(_, _)     => writes += 1
      case _                      =>
    }
    (comps, swaps, writes)