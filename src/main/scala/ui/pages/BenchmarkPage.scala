package ui.pages

import benchmark.{BenchmarkResult, BenchmarkRunner}
import model.{AlgorithmType, GeneratorType}
import ui.Theme
import scalafx.application.Platform
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.*
import scalafx.scene.layout.*
import scalafx.collections.ObservableBuffer
import scalafx.Includes.*

import scala.concurrent.{ExecutionContext, Future}

object BenchmarkPage:

  given ExecutionContext = ExecutionContext.global

  def build(): BorderPane =

    // ── Helpers ──────────────────────────────────────────────
    def hdr(text: String): Label =
      val l = new Label(text)
      l.style = Theme.titleStyle(9)
      l

    def spacer(h: Int = 6): Region =
      val r = new Region; r.prefHeight = h; r

    def sectionDiv(): Region =
      val r = new Region
      r.prefHeight = 1
      r.maxWidth   = Double.MaxValue
      r.style = s"-fx-background-color: ${Theme.BgBorder};"
      VBox.setMargin(r, Insets(12, 0, 12, 0))
      r

    def chip(text: String, color: String): Label =
      val l = new Label(text)
      l.style = s"-fx-background-color: ${color}22; -fx-text-fill: $color; " +
        s"-fx-padding: 2 7 2 7; -fx-background-radius: 3; " +
        s"-fx-font-family: 'Consolas', monospace; -fx-font-size: 10px;"
      l

    // ── Results buffer ────────────────────────────────────────
    val results = ObservableBuffer.empty[BenchmarkResult]

    // ── Config: algorithm checkboxes ──────────────────────────
    val algoChecks: Map[AlgorithmType, CheckBox] =
      AlgorithmType.values.map { a =>
        val cb = new CheckBox(a.label)
        cb.selected = true
        cb.style = Theme.labelStyle(11, Theme.TextNormal)
        a -> cb
      }.toMap

    // ── Config: pattern checkboxes ────────────────────────────
    val genChecks: Map[GeneratorType, CheckBox] =
      GeneratorType.values.map { g =>
        val cb = new CheckBox(g.label)
        cb.selected = g == GeneratorType.Random ||
          g == GeneratorType.Sorted  ||
          g == GeneratorType.SortedReverse
        cb.style = Theme.labelStyle(11, Theme.TextNormal)
        g -> cb
      }.toMap

    // ── Config: sizes ─────────────────────────────────────────
    val sizeChecks: Map[Int, CheckBox] =
      Seq(100, 500, 1000, 5000, 10000).map { n =>
        val cb = new CheckBox(n.toString)
        cb.selected = n <= 1000
        cb.style = Theme.labelStyle(11, Theme.TextNormal)
        n -> cb
      }.toMap

    // ── Config: options ───────────────────────────────────────
    val warmupCheck = new CheckBox("JIT Warmup (500 throwaway runs before measuring)")
    warmupCheck.selected = true
    warmupCheck.style = Theme.labelStyle(11, Theme.TextNormal)

    // ── Progress ──────────────────────────────────────────────
    val progressBar = new ProgressBar
    progressBar.progress = 0.0
    progressBar.prefWidth  = Double.MaxValue
    progressBar.maxWidth   = Double.MaxValue
    progressBar.style      = s"-fx-accent: ${Theme.AccentPrimary};"
    progressBar.visible    = false

    val progressLbl = new Label("Configure and run a benchmark")
    progressLbl.style = Theme.labelStyle(10, Theme.TextDim)

    // ── Buttons ───────────────────────────────────────────────
    val btnRun = new Button("▶  RUN BENCHMARK")
    btnRun.style    = Theme.buttonPrimary
    btnRun.maxWidth = Double.MaxValue

    val btnClear = new Button("✕  CLEAR RESULTS")
    btnClear.style    = Theme.buttonSecondary
    btnClear.maxWidth = Double.MaxValue

    btnClear.onAction = _ =>
      results.clear()
      progressLbl.text  = "Results cleared"
      progressBar.progress = 0.0

    // ── Table ─────────────────────────────────────────────────
    def strCol(title: String, w: Double, fn: BenchmarkResult => String): TableColumn[BenchmarkResult, String] =
      val c = new TableColumn[BenchmarkResult, String](title)
      c.prefWidth = w
      c.style = "-fx-alignment: CENTER;"
      c.cellValueFactory = cdf =>
        scalafx.beans.property.StringProperty(fn(cdf.value))
      c

    val table = new TableView[BenchmarkResult](results)
    table.style = s"-fx-background-color: ${Theme.BgBase}; -fx-text-fill: ${Theme.TextBright};"
    table.columnResizePolicy = TableView.ConstrainedResizePolicy
    VBox.setVgrow(table, Priority.Always)

    table.columns ++= Seq(
      strCol("Algorithm",   120, _.algoName),
      strCol("Pattern",     110, _.pattern),
      strCol("Size",         55, r => r.size.toString),
      strCol("Run",          50, r => if r.isWarm then "WARM" else "COLD"),
      strCol("Time",         80, _.timeMsStr),
      strCol("Throughput",   90, _.throughputStr),
      strCol("Comparisons",  95, r => f"${r.comparisons}%,d"),
      strCol("Swaps",        80, r => f"${r.swaps}%,d"),
      strCol("Writes",       75, r => f"${r.writes}%,d"),
      strCol("Heap Δ MB",    75, r => f"${r.heapDeltaMb}%.2f"),
      strCol("Alloc MB/s",   80, r => f"${r.allocRateMbS}%.1f"),
      strCol("GC runs",      60, r => r.gcCollections.toString),
      strCol("GC ms",        60, r => r.gcPauseMs.toString),
      strCol("Sorted?",      60, r => if r.isSorted then "✓" else "✗"),
      strCol("Stable?",      60, r => if r.isStable then "✓" else "—")
    )

    // ── Summary panel ─────────────────────────────────────────
    val summaryBox = new VBox(4)
    summaryBox.padding = Insets(10, 14, 10, 14)
    summaryBox.style   = s"-fx-background-color: ${Theme.BgBase}; " +
      s"-fx-border-color: ${Theme.BgBorder}; -fx-border-width: 1 0 0 0;"
    summaryBox.minHeight = 60

    def updateSummary(): Unit =
      summaryBox.children.clear()
      if results.isEmpty then return

      val warm = results.filter(_.isWarm)
      if warm.isEmpty then return

      val byAlgo = warm.groupBy(_.algoName)

      // Fastest and slowest
      val fastest = warm.minBy(_.timeNs)
      val slowest = warm.maxBy(_.timeNs)
      val mostComps = warm.maxBy(_.comparisons)
      val fewestComps = warm.minBy(_.comparisons)

      val row1 = new HBox(16)
      row1.alignment = Pos.CenterLeft
      row1.children.addAll(
        new Label("SUMMARY  "):
          style = Theme.titleStyle(9),
          chip(s"Fastest: ${fastest.algoName} (${fastest.timeMsStr})", Theme.AccentSuccess),
        chip(s"Slowest: ${slowest.algoName} (${slowest.timeMsStr})", Theme.AccentDanger),
        chip(s"Most comparisons: ${mostComps.algoName} (${f"${mostComps.comparisons}%,d"})", Theme.AccentSecondary),
        chip(s"Fewest comparisons: ${fewestComps.algoName} (${f"${fewestComps.comparisons}%,d"})", Theme.AccentPrimary)
      )
      summaryBox.children.add(row1.delegate)

      // Cold vs warm speedup per algo
      val speedupRow = new HBox(8)
      speedupRow.alignment = Pos.CenterLeft
      speedupRow.padding = Insets(4, 0, 0, 0)
      val speedupLbl = new Label("JIT SPEEDUP  ")
      speedupLbl.style = Theme.titleStyle(9)
      speedupRow.children.add(speedupLbl.delegate)

      results.groupBy(_.algoName).foreach { (name, runs) =>
        val cold = runs.find(!_.isWarm)
        val warmRuns = runs.filter(_.isWarm)
        if cold.isDefined && warmRuns.nonEmpty then
          val avgWarmNs = warmRuns.map(_.timeNs).sum / warmRuns.size
          val speedup   = cold.get.timeNs.toDouble / avgWarmNs
          val color     = if speedup > 3 then Theme.AccentSuccess
          else if speedup > 1.5 then Theme.AccentSecondary
          else Theme.TextDim
          speedupRow.children.add(chip(f"$name: ${speedup}%.1fx", color))
      }
      summaryBox.children.add(speedupRow.delegate)

    results.onChange { (_, _) =>
      Platform.runLater(updateSummary())
    }

    // ── Run logic ─────────────────────────────────────────────
    btnRun.onAction = _ =>
      val algos   = algoChecks.filter(_._2.selected.value).keys.toList
      val gens    = genChecks.filter(_._2.selected.value).keys.toList
      val sizes   = sizeChecks.filter(_._2.selected.value).keys.toList.sorted
      val doWarmup = warmupCheck.selected.value

      if algos.isEmpty || gens.isEmpty || sizes.isEmpty then
        progressLbl.text = "Select at least one algorithm, pattern, and size"
      else
        btnRun.disable   = true
        btnClear.disable = true
        progressBar.visible = true
        progressBar.progress = 0

        val total = algos.size * gens.size * sizes.size
        var done  = 0

        Future {
          for
            size <- sizes
            algo <- algos
            gen  <- gens
          do
            val config = BenchmarkRunner.RunConfig(
              algo    = algo,
              generator = gen,
              size    = size,
              warmup  = doWarmup
            )

            val runs = BenchmarkRunner.run(
              config,
              msg => Platform.runLater { progressLbl.text = msg }
            )

            done += 1
            Platform.runLater {
              results.addAll(runs*)
              progressBar.progress = done.toDouble / total
              progressLbl.text = s"$done / $total combinations complete"
            }

          Platform.runLater {
            btnRun.disable   = false
            btnClear.disable = false
            progressBar.visible = false
            progressLbl.text = s"Done — ${results.size} data points collected"
          }
        }

    // ── Config panel (left sidebar) ───────────────────────────
    val algoFlow = new FlowPane(8, 6)
    algoFlow.padding = Insets(4)
    algoChecks.values.foreach(cb => algoFlow.children.add(cb.delegate))

    val genFlow = new FlowPane(8, 6)
    genFlow.padding = Insets(4)
    genChecks.values.foreach(cb => genFlow.children.add(cb.delegate))

    val sizeFlow = new FlowPane(8, 6)
    sizeFlow.padding = Insets(4)
    sizeChecks.values.toSeq.sortBy(_.text.value.toInt)
      .foreach(cb => sizeFlow.children.add(cb.delegate))

    val configPanel = new VBox(0)
    configPanel.prefWidth  = 280
    configPanel.minWidth   = 280
    configPanel.maxWidth   = 280
    configPanel.style = s"-fx-background-color: ${Theme.BgBase}; " +
      s"-fx-border-color: ${Theme.BgBorder}; -fx-border-width: 0 1 0 0;"
    configPanel.padding = Insets(16, 14, 16, 14)

    val algoHdr = hdr("ALGORITHMS")
    val genHdr  = hdr("DATA PATTERNS")
    val sizeHdr = hdr("ARRAY SIZES")
    val optHdr  = hdr("OPTIONS")

    VBox.setMargin(genHdr,  Insets(12, 0, 4, 0))
    VBox.setMargin(sizeHdr, Insets(12, 0, 4, 0))
    VBox.setMargin(optHdr,  Insets(12, 0, 4, 0))

    val vGrow = new Region
    VBox.setVgrow(vGrow, Priority.Always)

    configPanel.children.addAll(
      algoHdr.delegate,
      spacer(4),
      algoFlow.delegate,
      genHdr.delegate,
      spacer(4),
      genFlow.delegate,
      sizeHdr.delegate,
      spacer(4),
      sizeFlow.delegate,
      optHdr.delegate,
      spacer(4),
      warmupCheck.delegate,
      vGrow,
      sectionDiv(),
      progressBar.delegate,
      spacer(4),
      progressLbl.delegate,
      spacer(8),
      btnRun.delegate,
      spacer(4),
      btnClear.delegate
    )

    // ── Right: table + summary ────────────────────────────────
    val tableBox = new VBox(0)
    tableBox.style = s"-fx-background-color: ${Theme.BgDeep};"
    VBox.setVgrow(table, Priority.Always)
    tableBox.children.addAll(table.delegate, summaryBox.delegate)
    VBox.setVgrow(tableBox, Priority.Always)
    HBox.setHgrow(tableBox, Priority.Always)

    val mainRow = new HBox
    mainRow.children.addAll(configPanel.delegate, tableBox.delegate)
    VBox.setVgrow(mainRow, Priority.Always)

    val page = new BorderPane
    page.style  = s"-fx-background-color: ${Theme.BgDeep};"
    page.center = mainRow
    page