package benchmark.pure

import algorithms.AlgorithmRegistry
import algorithms.purebenchmark.PureAlgorithmRegistry
import benchmark.{CorrectnessValidator, LatencyStats, StabilityChecker}
import model.{ArrayGenerator, SortStep}
import ui.utils.AlgorithmType

/** Pure benchmark engine — maximum timing accuracy, minimal metrics.
 *
 *  Guarantees:
 *  - The timing window contains ONLY sortFn(array) between two nanoTime() calls
 *  - Op counts are collected in a completely separate non-timed pass
 *  - No heap, GC, or CPU probing anywhere in this engine
 *
 *  Use FullBenchmarkEngine if you need system metrics. */
object PureBenchmarkEngine:

  /** Run a full pure benchmark for the given config.
   *  Returns Left(errorMessage) if correctness validation fails,
   *  Right(PureResult) on success. */
  def run(config: PureRunConfig): Either[String, PureResult] =
    val sortFn = PureAlgorithmRegistry.get(config.algo)

    // ── 1. Correctness pre-flight ─────────────────────────
    val checkInput  = ArrayGenerator.generate(config.generator, config.size)
    val checkWorking = checkInput.clone()
    sortFn(checkWorking)
    val reference = checkInput.clone()
    java.util.Arrays.sort(reference)
    if !java.util.Arrays.equals(checkWorking, reference) then
      return Left(s"${config.algo.label} produced incorrect output on ${config.generator.label}")

    // ── 2. Warmup — JIT reaches C2 before measurement ────
    Steps.warmup(sortFn, config.generator, config.size, config.warmupRounds)

    // ── 3. Measure — timing window contains only sortFn ──
    val samples = Steps.measure(sortFn, config.generator, config.size, config.measureRounds)

    // ── 4. Latency distribution from raw samples ─────────
    val stats = LatencyStats.compute(samples.toSeq)
    val meanMs = stats.mean / 1_000_000.0

    // ── 5. Op counts — separate non-timed pass ───────────
    val (comparisons, swaps, writes) = collectOpCounts(config)

    // ── 6. Correctness and stability ─────────────────────
    val finalInput  = ArrayGenerator.generate(config.generator, config.size)
    val finalWorking = finalInput.clone()
    sortFn(finalWorking)
    val ref2     = finalInput.clone()
    java.util.Arrays.sort(ref2)
    val isSorted = java.util.Arrays.equals(finalWorking, ref2)
    val isStable = StabilityChecker.isAlgorithmStable(config.algo)

    Right(PureResult(
      algoName    = config.algo.label,
      pattern     = config.generator.label,
      size        = config.size,
      meanNs      = stats.mean,
      medianNs    = stats.median,
      stdDevNs    = stats.stdDev,
      p90Ns       = stats.p90,
      p95Ns       = stats.p95,
      p99Ns       = stats.p99,
      minNs       = stats.min,
      maxNs       = stats.max,
      throughputElemsPerMs = if meanMs > 0 then config.size / meanMs else 0.0,
      comparisons  = comparisons,
      swaps        = swaps,
      writes       = writes,
      isSorted     = isSorted,
      isStable     = isStable,
      warmupRounds = config.warmupRounds,
      measureRounds = config.measureRounds
    ))

  /** Collect op counts by replaying the step-emitting algorithm once.
   *  This is intentionally outside the timing window. */
  private def collectOpCounts(config: PureRunConfig): (Long, Long, Long) =
    val arr = ArrayGenerator.generate(config.generator, config.size)
    var comparisons = 0L
    var swaps       = 0L
    var writes      = 0L
    AlgorithmRegistry.get(config.algo).steps(arr).foreach {
      case SortStep.Compare(_, _) => comparisons += 1
      case SortStep.Swap(_, _)    => swaps       += 1
      case SortStep.Set(_, _)     => writes      += 1
      case _                      =>
    }
    (comparisons, swaps, writes)