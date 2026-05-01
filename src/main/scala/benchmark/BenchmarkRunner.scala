package benchmark

import algorithms.pure.PureAlgorithmRegistry
import model.{AlgorithmType, ArrayGenerator, GeneratorType}

object BenchmarkRunner:

  private val WarmupRuns  = 5000
  private val MeasureRuns = 50

  case class GlobalWarmupKey(size: Int, pattern: GeneratorType)

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
                         warmup:    Boolean,
                         skipColdRun: Boolean = false  // Skip cold run if already warmed up globally
                       )

  /**
   * Warm up all algorithms for a specific array size and generator pattern.
   * This is called once per (size, generator) combination before any measurements.
   */
  def globalWarmup(
                    algorithms: Seq[AlgorithmType],
                    generator:  GeneratorType,
                    size:       Int,
                    onProgress: String => Unit
                  ): Unit =
    onProgress(s"Global JIT warmup for ${generator.label} / size=$size ($WarmupRuns iterations per algorithm)…")

    for algo <- algorithms do
      val sortFn = PureAlgorithmRegistry.get(algo)
      val warmupInputs = pregenerateInputs(generator, size, WarmupRuns)

      var wi = 0
      while wi < WarmupRuns do
        val arr = warmupInputs(wi).clone()
        sortFn(arr)
        wi += 1

    System.gc(); System.gc()
    System.runFinalization()
    Thread.sleep(200)

  def run(config: RunConfig, onProgress: String => Unit): Seq[BenchmarkResult] =
    val results = collection.mutable.ArrayBuffer.empty[BenchmarkResult]
    val sortFn  = PureAlgorithmRegistry.get(config.algo)

    // ── Pre-flight correctness check ──────────────────────────
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
        isSorted   = false,
        //isStable   = StabilityChecker.isAlgorithmStable(config.algo.label),
        failureMsg = s"Correctness check failed — output does not match reference sort"
       ))

    // ── Cold run ──────────────────────────────────────────────
    val coldInput = ArrayGenerator.generate(config.generator, config.size)
    if !config.skipColdRun then
      results += measureSingle(
        config.algo, config.generator, config.size,
        coldInput.clone(), coldInput, sortFn, isWarm = false
      )

    if !config.warmup then return results.toSeq

    // ── Warmup (skipped if global warmup was already done) ────
    if !config.skipColdRun then
      onProgress(s"Warming up ${config.algo.label} ($WarmupRuns iterations)…")
      val warmupInputs = pregenerateInputs(config.generator, config.size, WarmupRuns)
      var wi = 0
      while wi < WarmupRuns do
        val arr = warmupInputs(wi).clone()
        sortFn(arr)
        wi += 1

      System.gc(); System.gc()
      System.runFinalization()
      Thread.sleep(200)

    // ── Pre-generate measurement inputs ───────────────────────
    onProgress(s"Preparing $MeasureRuns measurement arrays…")
    val measureInputs = pregenerateInputs(config.generator, config.size, MeasureRuns)

    System.gc()
    Thread.sleep(100)

    // ── Warm measurement ──────────────────────────────────────
    onProgress(s"Measuring ${config.algo.label} ($MeasureRuns warm runs)…")
    val samples = new Array[BenchmarkResult](MeasureRuns)
    var mi = 0
    while mi < MeasureRuns do
      val original = measureInputs(mi)
      val toSort   = original.clone()           // what gets sorted
      samples(mi)  = measureSingle(
        config.algo, config.generator, config.size,
        toSort, original, sortFn, isWarm = true
      )
      mi += 1

    // ── Collect op counts AFTER all timing ───────────────────
    val (comparisons, swaps, writes) =
      collectOpCounts(config.algo, config.generator, config.size)

    val times   = samples.map(_.timeNs).toSeq
    val stats   = LatencyStats.compute(times)
    val timeMs  = stats.mean / 1_000_000.0
    //val stable  = StabilityChecker.isAlgorithmStable(config.algo.label)

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
      maxHeapMb     = samples.map(_.maxHeapMb).sum / MeasureRuns,
      allocRateMbS  = samples.map(_.allocRateMbS).sum / MeasureRuns,
      gcCollections = samples.map(_.gcCollections).sum / MeasureRuns,
      gcPauseMs     = samples.map(_.gcPauseMs).sum / MeasureRuns,
      cpuTimeNs     = samples.map(_.cpuTimeNs).sum / MeasureRuns,
      userTimeNs    = samples.map(_.userTimeNs).sum / MeasureRuns,
      cpuPercent    = if stats.mean > 0 then
        (samples.map(_.cpuTimeNs).sum / MeasureRuns).toDouble / stats.mean * 100
      else 0.0,
      isSorted      = samples.forall(_.isSorted),
      //isStable      = stable
    )

    // Only add the aggregate — skip individual samples to reduce table noise
    // Individual samples are used only for statistics, not displayed
    results += aggregate
    results.toSeq

  /**
   * @param toSort   fresh clone of original — will be sorted in place
   * @param original untouched original — used for reference correctness check
   */
  private def measureSingle(
                             algo:     AlgorithmType,
                             gen:      GeneratorType,
                             size:     Int,
                             toSort:   Array[Int],   // mutable — sorted in place
                             original: Array[Int],   // immutable reference — NOT modified
                             sortFn:   Array[Int] => Unit,
                             isWarm:   Boolean
                           ): BenchmarkResult =
    val snapBefore              = SystemMonitor.snapshot()
    val (cpuBefore, userBefore) = CpuMonitor.currentThreadTimes()

    // ══ TIMED SECTION ════════════════════════════════════════
    val t0 = System.nanoTime()
    sortFn(toSort)
    val timeNs = System.nanoTime() - t0
    // ══ END TIMED SECTION ════════════════════════════════════

    val (cpuAfter, userAfter) = CpuMonitor.currentThreadTimes()
    val snapAfter             = SystemMonitor.snapshot()

    val heapDelta = (snapAfter.heapMb        - snapBefore.heapMb).max(0.0)
    val gcDelta   = (snapAfter.gcCollections  - snapBefore.gcCollections).max(0)
    val gcTime    = (snapAfter.gcTimeMs       - snapBefore.gcTimeMs).max(0)
    val cpuDelta  = (cpuAfter  - cpuBefore).max(0)
    val userDelta = (userAfter - userBefore).max(0)
    val timeMs    = timeNs / 1_000_000.0

    // Correctness: compare sorted result against reference sort of original
    val ref       = original.clone()
    java.util.Arrays.sort(ref)
    val isSorted  = java.util.Arrays.equals(toSort, ref)

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
      comparisons   = 0L,  // filled in aggregate only
      swaps         = 0L,
      writes        = 0L,
      maxHeapMb     = heapDelta,
      allocRateMbS  = if timeMs > 0 then heapDelta / (timeMs / 1000.0) else 0.0,
      gcCollections = gcDelta,
      gcPauseMs     = gcTime,
      cpuTimeNs     = cpuDelta,
      userTimeNs    = userDelta,
      cpuPercent    = if timeNs > 0 then cpuDelta.toDouble / timeNs * 100 else 0.0,
      isSorted      = isSorted,
      //isStable      = StabilityChecker.isAlgorithmStable(algo.label)
    )

  private def collectOpCounts(
                               algo: AlgorithmType,
                               gen:  GeneratorType,
                               size: Int
                             ): (Long, Long, Long) =
    import algorithms.AlgorithmRegistry
    import model.SortStep
    val arr = ArrayGenerator.generate(gen, size)
    var comps = 0L; var swaps = 0L; var writes = 0L
    AlgorithmRegistry.get(algo).steps(arr).foreach {
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