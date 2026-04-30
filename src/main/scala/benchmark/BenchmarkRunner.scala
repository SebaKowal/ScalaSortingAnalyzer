package benchmark

import algorithms.pure.PureAlgorithmRegistry
import model.{AlgorithmType, ArrayGenerator, GeneratorType}

object BenchmarkRunner:

  // 2000 warmup: enough for JIT C1→C2 transition
  // C2 typically kicks in at ~10k invocations of a method
  // but the sort body itself needs ~1k-2k to fully optimize
  private val WarmupRuns  = 5000
  private val MeasureRuns = 50    // more samples = tighter confidence intervals

  // Pre-generate all input arrays ONCE before any timing
  // Avoids Random.nextInt() synchronized calls between runs
  // Avoids cache pollution from generation between measurements
  private def pregenerateInputs(
                                 generator: GeneratorType,
                                 size:      Int,
                                 count:     Int
                               ): Array[Array[Int]] =
    Array.tabulate(count)(_ => ArrayGenerator.generate(generator, size))

  case class RunConfig(
                        algo:      AlgorithmType,
                        generator: GeneratorType,
                        size:      Int,
                        warmup:    Boolean
                      )

  def run(config: RunConfig, onProgress: String => Unit): Seq[BenchmarkResult] =
    val results = collection.mutable.ArrayBuffer.empty[BenchmarkResult]
    val sortFn  = PureAlgorithmRegistry.get(config.algo)

    // ── Pre-flight correctness — NOT timed, NOT counted in metrics
    val checkInput   = ArrayGenerator.generate(config.generator, config.size)
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
        failureMsg = "Correctness check failed — output does not match reference sort"
      ))

    // ── Cold run — capture pre-JIT baseline
    val coldInput = ArrayGenerator.generate(config.generator, config.size)
    results += measureSingle(config.algo, config.generator, config.size,
      coldInput, sortFn, isWarm = false)

    if !config.warmup then return results.toSeq

    // ── Warmup — vary input to prevent JIT from constant-folding
    onProgress(s"Warming up ${config.algo.label} (${WarmupRuns} iterations)…")
    val warmupInputs = pregenerateInputs(config.generator, config.size, WarmupRuns)
    var wi = 0
    while wi < WarmupRuns do
      val arr = warmupInputs(wi).clone()  // clone so each warmup gets a fresh copy
      sortFn(arr)
      wi += 1

    // ── GC stabilization before measurement
    // Request GC twice — G1 sometimes needs two requests
    System.gc(); System.gc()
    System.runFinalization()
    // Wait for GC to complete — Thread.sleep is not precise but sufficient
    // The key is to not start timing immediately after GC request
    Thread.sleep(200)

    // ── Pre-generate measurement inputs BEFORE any timing starts
    // This ensures cache state is stable when timing begins
    onProgress(s"Preparing ${MeasureRuns} measurement arrays…")
    val measureInputs = pregenerateInputs(config.generator, config.size, MeasureRuns)

    // Final GC after input generation — clear any generation-related garbage
    System.gc()
    Thread.sleep(100)

    // ── Warm measurement — tight loop, minimal overhead
    onProgress(s"Measuring ${config.algo.label} (${MeasureRuns} warm runs)…")
    val samples = new Array[BenchmarkResult](MeasureRuns)
    var mi = 0
    while mi < MeasureRuns do
      // Clone pre-generated input — avoids Random.nextInt between runs
      val arr = measureInputs(mi).clone()
      samples(mi) = measureSingle(config.algo, config.generator, config.size,
        arr, sortFn, isWarm = true)
      mi += 1

    // ── Statistical aggregation
    val times     = samples.map(_.timeNs).toSeq
    val stats     = LatencyStats.compute(times)
    val timeMs    = stats.mean / 1_000_000.0

    // ── Collect algorithmic op counts SEPARATELY — never during timing
    // Run visualization layer ONCE after all timing is complete
    // This way GC pressure from SortStep allocations cannot affect measurements
    val (comparisons, swaps, writes) = collectOpCounts(config.algo, config.generator, config.size)

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
      comparisons   = comparisons,
      swaps         = swaps,
      writes        = writes,
      //heapDeltaMb   = samples.map(_.maxHeapMb).sum / MeasureRuns,
      allocRateMbS  = samples.map(_.allocRateMbS).sum / MeasureRuns,
      gcCollections = samples.map(_.gcCollections).sum / MeasureRuns,
      gcPauseMs     = samples.map(_.gcPauseMs).sum / MeasureRuns,
      cpuTimeNs     = samples.map(_.cpuTimeNs).sum / MeasureRuns,
      userTimeNs    = samples.map(_.userTimeNs).sum / MeasureRuns,
      cpuPercent    = if stats.mean > 0 then
        (samples.map(_.cpuTimeNs).sum / MeasureRuns).toDouble / stats.mean * 100
      else 0.0,
      isSorted      = samples.forall(_.isSorted),
      //isStable      = StabilityChecker.isAlgorithmStable(config.algo.label)
    )

    results ++= samples
    results += aggregate
    results.toSeq

  private def measureSingle(
                             algo:    AlgorithmType,
                             gen:     GeneratorType,
                             size:    Int,
                             input:   Array[Int],   // pre-cloned — ready to sort
                             sortFn:  Array[Int] => Unit,
                             isWarm:  Boolean
                           ): BenchmarkResult =
    // input is already a fresh clone — sort directly, no additional clone
    val snapBefore              = SystemMonitor.snapshot()
    val (cpuBefore, userBefore) = CpuMonitor.currentThreadTimes()

    // ══ TIMED SECTION START ══════════════════════════════════
    val t0 = System.nanoTime()
    sortFn(input)
    val timeNs = System.nanoTime() - t0
    // ══ TIMED SECTION END ════════════════════════════════════

    val (cpuAfter, userAfter) = CpuMonitor.currentThreadTimes()
    val snapAfter             = SystemMonitor.snapshot()

    val heapDelta = (snapAfter.heapMb       - snapBefore.heapMb).max(0.0)
    val gcDelta   = (snapAfter.gcCollections - snapBefore.gcCollections).max(0)
    val gcTime    = (snapAfter.gcTimeMs      - snapBefore.gcTimeMs).max(0)
    val cpuDelta  = (cpuAfter  - cpuBefore).max(0)
    val userDelta = (userAfter - userBefore).max(0)
    val timeMs    = timeNs / 1_000_000.0

    // Correctness check after timing — uses original input for reference
    // input is now sorted in place
    val isSortedCorrectly = isSortedAsc(input)

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
      // Op counts are filled in by aggregate — 0 for individual samples
      comparisons   = 0L,
      swaps         = 0L,
      writes        = 0L,
      maxHeapMb     = heapDelta,
      allocRateMbS  = if timeMs > 0 then heapDelta / (timeMs / 1000.0) else 0.0,
      gcCollections = gcDelta,
      gcPauseMs     = gcTime,
      cpuTimeNs     = cpuDelta,
      userTimeNs    = userDelta,
      cpuPercent    = if timeNs > 0 then cpuDelta.toDouble / timeNs * 100 else 0.0,
      isSorted      = isSortedCorrectly,
      //isStable      = StabilityChecker.isAlgorithmStable(algo.label)
    )

  /**
   * Collect comparison/swap/write counts using the visualization layer.
   * Called AFTER all timing measurements are complete.
   * GC pressure from SortStep allocations cannot affect timed results.
   * Called ONCE per (algo, generator, size) combination — not per warm run.
   */
  private def collectOpCounts(
                               algo: AlgorithmType,
                               gen:  GeneratorType,
                               size: Int
                             ): (Long, Long, Long) =
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

  @inline private def isSortedAsc(arr: Array[Int]): Boolean =
    if arr.length <= 1 then return true
    var i = 1
    while i < arr.length do
      if arr(i) < arr(i - 1) then return false
      i += 1
    true