package ui.benchmarking

import benchmark.full.{FullBenchmarkEngine, FullRunConfig, FullResult}
import benchmark.pure.{PureBenchmarkEngine, PureRunConfig, PureResult}
import benchmark.{CorrectnessValidator, BenchmarkExporter}
import model.GeneratorType
import scalafx.application.Platform
import scalafx.collections.ObservableBuffer
import ui.utils.AlgorithmType
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

final case class BenchmarkUiState(
                                   isRunning: Boolean = false,
                                   progress:  Double  = 0.0,
                                   status:    String  = "Configure options and click RUN",
                                   isError:   Boolean = false
                                 )

enum BenchmarkMode:
  case Pure, Full

class BenchmarkController(
                           val results:   ObservableBuffer[AnyRef],
                           onStateChange: BenchmarkUiState => Unit,
                           onResults:     Seq[AnyRef]      => Unit
                         )(using ec: ExecutionContext):

  private var state = BenchmarkUiState()

  private def setState(s: BenchmarkUiState): Unit =
    state = s
    Platform.runLater { onStateChange(s) }

  def setProgress(msg: String, frac: Double): Unit =
    setState(state.copy(status = msg, progress = frac))

  // ── Export Logic ──────────────────────────────────────────

  def exportExcel(): Unit =
    if results.nonEmpty then
      new java.io.File("exports").mkdirs()

      val fulls = results.toSeq.collect { case r: FullResult => r }
      val pures = results.toSeq.collect { case r: PureResult => r }

      val path = if fulls.nonEmpty then
        BenchmarkExporter.exportFullExcel(fulls, "exports")
      else
        BenchmarkExporter.exportPureExcel(pures, "exports")

      println(s"Excel exported to: $path")

  def exportJson(): Unit =
    if results.nonEmpty then
      new java.io.File("exports").mkdirs()
      // JSON obsłuży oba typy dzięki pattern matchingowi wewnątrz eksportera
      val path = BenchmarkExporter.exportJson(results.toSeq, "exports")
      println(s"JSON exported to: $path")

  // ── Rank helpers ──────────────────────────────────────────

  def computeRanks(): Map[(String, String, Int), Int] =
    val items = asPureResults
    val grouped  = items.groupBy(r => (r.pattern, r.algoName, r.size))
    val avgTimes = grouped.map((k, rs) => k -> rs.map(_.meanNs).sum / rs.size)
    val byBucket = avgTimes.groupBy { case ((p, _, s), _) => (p, s) }
    byBucket.flatMap { case (_, entries) =>
      entries.toSeq.sortBy(_._2).zipWithIndex.map {
        case (((p, algo, s), _), idx) => (algo, p, s) -> (idx + 1)
      }
    }

  def pctVsFastest(r: PureResult): String =
    val peers = asPureResults.filter(p => p.pattern == r.pattern && p.size == r.size)
    if peers.isEmpty then "—"
    else
      val fastest = peers.map(_.meanNs).min
      if fastest <= 0 then "—"
      else
        val ratio = r.meanNs.toDouble / fastest
        if ratio <= 1.05 then "fastest" else f"$ratio%.1fx"

  private def asPureResults: Seq[PureResult] =
    results.toSeq.collect {
      case r: PureResult => r
      case r: FullResult => r.pure
    }

  // ── Actions ───────────────────────────────────────────────

  def clearResults(): Unit =
    results.clear()
    setState(BenchmarkUiState(status = "Results cleared"))

  def validate(onDone: (Int, Int) => Unit): Unit =
    setState(state.copy(isRunning = true, status = "Running validation…"))
    Future { CorrectnessValidator.validateAll() }.onComplete {
      case Success(vr) =>
        val passed = vr.count(_.passed)
        val failed = vr.count(!_.passed)
        Platform.runLater {
          setState(BenchmarkUiState(
            status  = if failed == 0 then s"✓ All $passed checks passed" else s"✗ $failed failed / $passed passed",
            isError = failed > 0))
          onDone(passed, failed)
        }
      case Failure(ex) =>
        Platform.runLater {
          setState(BenchmarkUiState(isError = true, status = s"Validation error: ${ex.getMessage}"))
          onDone(0, -1)
        }
    }

  def runBenchmarks(
                     algos:         List[AlgorithmType],
                     gens:          List[GeneratorType],
                     sizes:         List[Int],
                     mode:          BenchmarkMode,
                     validateFirst: Boolean,
                     probeHeap:     Boolean,
                     probeCpu:      Boolean,
                     probeGc:       Boolean,
                     warmupRounds:  Int,
                     measureRounds: Int,
                     onDone:        Int => Unit
                   ): Unit =
    if algos.isEmpty || gens.isEmpty || sizes.isEmpty then
      setState(state.copy(status = "Select at least one algorithm, pattern, and size"))
      return

    setState(BenchmarkUiState(isRunning = true, status = "Starting…", progress = 0.0))

    val total = algos.size * gens.size * sizes.size
    var done  = 0

    val benchStep = Future {
      if validateFirst then
        val failures = CorrectnessValidator.validateAll().filterNot(_.passed)
        if failures.nonEmpty then throw RuntimeException(s"${failures.size} checks failed")

      for
        size <- sizes
        gen  <- gens
        algo <- algos
      do
        val result: AnyRef = mode match
          case BenchmarkMode.Pure =>
            val cfg = PureRunConfig(algo, gen, size, warmupRounds, measureRounds)
            PureBenchmarkEngine.run(cfg).getOrElse(failedPureResult(algo, gen, size, warmupRounds, measureRounds, "Error"))

          case BenchmarkMode.Full =>
            val cfg = FullRunConfig(algo, gen, size, warmupRounds, measureRounds, probeHeap, probeCpu, probeGc)
            FullBenchmarkEngine.run(cfg).getOrElse(FullResult(pure = failedPureResult(algo, gen, size, warmupRounds, measureRounds, "Error")))

        done += 1
        val frac = done.toDouble / total
        Platform.runLater {
          onResults(Seq(result))
          setProgress(s"$done / $total complete", frac)
        }
    }

    benchStep.onComplete {
      case Success(_) =>
        Platform.runLater {
          setState(BenchmarkUiState(status = s"Done — ${results.size} results"))
          onDone(results.size)
        }
      case Failure(ex) =>
        Platform.runLater {
          setState(BenchmarkUiState(isError = true, status = ex.getMessage))
          onDone(results.size)
        }
    }

  private def failedPureResult(
                                algo: AlgorithmType, gen: GeneratorType, size: Int,
                                warmup: Int, measure: Int, msg: String
                              ): PureResult =
    PureResult(
      algoName = algo.label, pattern = gen.label, size = size,
      meanNs = 0, medianNs = 0, stdDevNs = 0,
      p90Ns = 0, p95Ns = 0, p99Ns = 0, minNs = 0, maxNs = 0,
      throughputElemsPerMs = 0, comparisons = 0, swaps = 0, writes = 0,
      isSorted = false, isStable = false,
      warmupRounds = warmup, measureRounds = measure,
      failureMsg = msg
    )