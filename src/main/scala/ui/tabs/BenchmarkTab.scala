package ui.tabs

import app.AppState
import benchmark.{BenchmarkResult, SystemMonitor}
import model.{AlgorithmType, ArrayGenerator, GeneratorType}
import algorithms.AlgorithmRegistry
import scalafx.application.Platform
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.*
import scalafx.scene.layout.*
import scalafx.collections.ObservableBuffer
import scalafx.Includes.*

import scala.concurrent.{ExecutionContext, Future}

class BenchmarkTab(state: AppState):
  given ExecutionContext = ExecutionContext.global

  private val results = ObservableBuffer.empty[BenchmarkResult]

  private def makeStringCol(title: String, width: Double, extract: BenchmarkResult => String): TableColumn[BenchmarkResult, String] =
    val c = new TableColumn[BenchmarkResult, String](title)
    c.prefWidth = width
    c.style = "-fx-alignment: CENTER;"
    c.cellValueFactory = { cdf =>
      scalafx.beans.property.StringProperty(extract(cdf.value))
    }
    c

  private val table = new TableView[BenchmarkResult](results)
  table.style = "-fx-background-color: #12142b; -fx-text-fill: #e0e0ff;"
  table.columnResizePolicy = TableView.ConstrainedResizePolicy
  table.columns ++= Seq(
    makeStringCol("Algorithm",   140, _.algorithmName),
    makeStringCol("Size",         60, r => r.arraySize.toString),
    makeStringCol("Pattern",     110, _.generatorType),
    makeStringCol("Comparisons",  90, r => f"${r.comparisons}%,d"),
    makeStringCol("Swaps",        80, r => f"${r.swaps}%,d"),
    makeStringCol("Time (ms)",    80, r => r.elapsedMs.toString),
    makeStringCol("Heap Δ (MB)", 80,  r => f"${r.memoryUsedMb}%.2f"),
    makeStringCol("GC runs",      70, r => r.gcCollections.toString),
    makeStringCol("GC time (ms)", 90, r => r.gcTimeMs.toString),
    makeStringCol("CPU %",        70, r => f"${r.cpuLoadPercent}%.1f")
  )

  private val progressBar = new ProgressBar
  progressBar.progress = 0.0
  progressBar.prefWidth = Double.MaxValue
  progressBar.style = "-fx-accent: #7986cb;"
  progressBar.visible = false

  private val statusLbl = new Label("Select algorithms and click Run Benchmark"):
    style = "-fx-text-fill: #7986cb; -fx-font-size: 12px;"

  private val algoChecks: Map[AlgorithmType, CheckBox] =
    AlgorithmType.values.map { a =>
      a -> new CheckBox(a.label):
        selected = true
        style = "-fx-text-fill: #b0b8d8; -fx-font-size: 11px;"
    }.toMap

  private val genChecks: Map[GeneratorType, CheckBox] =
    GeneratorType.values.map { g =>
      g -> new CheckBox(g.label):
        selected = (g == GeneratorType.Random)
        style = "-fx-text-fill: #b0b8d8; -fx-font-size: 11px;"
    }.toMap

  private val sizeField = new TextField
  sizeField.text = "100"
  sizeField.prefWidth = 80
  sizeField.style = "-fx-background-color: #1a1d2e; -fx-text-fill: #e0e0ff; -fx-border-color: #2a2d45;"

  private val iterField = new TextField
  iterField.text = "3"
  iterField.prefWidth = 80
  iterField.style = "-fx-background-color: #1a1d2e; -fx-text-fill: #e0e0ff; -fx-border-color: #2a2d45;"

  private val btnRun = new Button("▶  Run Benchmark"):
    style = "-fx-background-color: #7986cb; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 16;"

  private val btnClear = new Button("✕  Clear"):
    style = "-fx-background-color: #2a2d45; -fx-text-fill: #b0b8d8; -fx-padding: 6 12;"

  btnClear.onAction = _ => results.clear()
  btnRun.onAction   = _ => runBenchmark()

  private def runBenchmark(): Unit =
    val size       = sizeField.text.value.toIntOption.getOrElse(100).min(5000)
    val iterations = iterField.text.value.toIntOption.getOrElse(3).min(10)
    val algos      = algoChecks.filter(_._2.selected.value).keys.toList
    val gens       = genChecks.filter(_._2.selected.value).keys.toList
    if algos.isEmpty || gens.isEmpty then return

    btnRun.disable = true
    progressBar.visible = true
    val total = algos.size * gens.size * iterations
    var done  = 0

    Future {
      for
        algo <- algos
        gen  <- gens
        _    <- 0 until iterations
      do
        val arr  = ArrayGenerator.generate(gen, size)
        val snap = SystemMonitor.snapshot()
        val t0   = System.currentTimeMillis()
        var comps = 0L
        var swps  = 0L

        AlgorithmRegistry.get(algo).steps(arr).foreach {
          case model.SortStep.Compare(_, _) => comps += 1
          case model.SortStep.Swap(_, _)    => swps  += 1
          case model.SortStep.Set(_, _)     => swps  += 1
          case _                            =>
        }

        val elapsed     = System.currentTimeMillis() - t0
        val snapAfter   = SystemMonitor.snapshot()
        val gcDelta     = (snapAfter.gcCollections - snap.gcCollections).max(0)
        val gcTimeDelta = (snapAfter.gcTimeMs      - snap.gcTimeMs).max(0)
        val heapDelta   = (snapAfter.heapMb        - snap.heapMb).max(0.0)
        val cpu         = SystemMonitor.cpuLoadPercent

        val result = BenchmarkResult(
          algorithmName  = algo.label,
          arraySize      = size,
          generatorType  = gen.label,
          comparisons    = comps,
          swaps          = swps,
          elapsedMs      = elapsed,
          memoryUsedMb   = heapDelta,
          gcCollections  = gcDelta,
          gcTimeMs       = gcTimeDelta,
          cpuLoadPercent = cpu
        )

        done += 1
        Platform.runLater {
          results += result
          progressBar.progress = done.toDouble / total
          statusLbl.text = s"$done / $total runs complete"
        }

      Platform.runLater {
        btnRun.disable = false
        progressBar.visible = false
        statusLbl.text = s"Benchmark complete — ${results.size} results"
      }
    }

  private def sectionLabel(text: String): Label = new Label(text):
    style = "-fx-text-fill: #7986cb; -fx-font-size: 12px; -fx-font-weight: bold;"

  def build(): VBox =
    val algoGrid = new FlowPane(8, 6)
    algoGrid.padding = Insets(4)
    algoChecks.values.foreach(cb => algoGrid.children.add(cb.delegate))

    val genGrid = new FlowPane(8, 6)
    genGrid.padding = Insets(4)
    genChecks.values.foreach(cb => genGrid.children.add(cb.delegate))

    val configRow = new HBox(24)
    configRow.padding = Insets(8, 0, 8, 0)
    configRow.alignment = Pos.CenterLeft
    configRow.children.addAll(
      new VBox(4, sectionLabel("Array Size"), sizeField).delegate,
      new VBox(4, sectionLabel("Iterations"), iterField).delegate,
      new VBox(8, btnRun, btnClear).delegate
    )

    val controls = new VBox(8)
    controls.padding = Insets(12)
    controls.style = "-fx-background-color: #1a1d2e; -fx-border-color: #2a2d45; -fx-border-radius: 6;"
    controls.children.addAll(
      sectionLabel("Algorithms").delegate,
      algoGrid.delegate,
      sectionLabel("Data Patterns").delegate,
      genGrid.delegate,
      configRow.delegate,
      progressBar.delegate,
      statusLbl.delegate
    )

    val box = new VBox(10)
    box.padding = Insets(12)
    box.style = "-fx-background-color: #0f111a;"
    box.children.addAll(controls.delegate, table.delegate)
    VBox.setVgrow(table, Priority.Always)
    box