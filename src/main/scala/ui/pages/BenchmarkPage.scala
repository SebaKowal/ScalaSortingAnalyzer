package ui.pages

import benchmark.{BenchmarkExporter, BenchmarkResult, BenchmarkRunner, CorrectnessValidator}
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
  private val benchmarkExecutor =
    java.util.concurrent.Executors.newSingleThreadExecutor { r =>
      val t = new Thread(r, "benchmark-worker")
      t.setDaemon(true)
      t
    }
  given ExecutionContext = ExecutionContext.fromExecutorService(benchmarkExecutor)

  def build(results: ObservableBuffer[BenchmarkResult]): BorderPane =

    def hdr(text: String): Label =
      val l = new Label(text); l.style = Theme.titleStyle(9); l

    def spacer(h: Int = 6): Region =
      val r = new Region; r.prefHeight = h; r

    def divider(): Region =
      val r = new Region
      r.prefHeight = 1; r.maxWidth = Double.MaxValue
      r.style = s"-fx-background-color: ${Theme.BgBorder};"
      VBox.setMargin(r, Insets(10, 0, 10, 0)); r

    def chip(text: String, color: String): Label =
      val l = new Label(text)
      l.style = s"-fx-background-color: ${color}22; -fx-text-fill: $color; " +
        s"-fx-padding: 2 7 2 7; -fx-background-radius: 3; " +
        s"-fx-font-family: 'Consolas', monospace; -fx-font-size: 10px;"
      l

    // ── Rank / pct helpers ────────────────────────────────────
    def rankMap(): Map[(String, String, Int), Int] =
      val warm = results.filter(_.isWarm)
      val grouped = warm.groupBy(r => (r.pattern, r.algoName, r.size))
      val avgTimes = grouped.map { (k, runs) => k -> runs.map(_.timeNs).sum / runs.size }
      val byBucket = avgTimes.groupBy { case ((pattern, _, size), _) => (pattern, size) }
      byBucket.flatMap { case ((pattern, size), entries) =>
        entries.toSeq.sortBy(_._2).zipWithIndex.map {
          case (((p, algo, s), _), idx) => (algo, p, s) -> (idx + 1)
        }
      }

    def pctVsFastest(r: BenchmarkResult): String =
      if !r.isWarm then "—"
      else
        val warm = results.filter(w => w.isWarm && w.pattern == r.pattern && w.size == r.size)
        if warm.isEmpty then "—"
        else
          val fastest = warm.map(_.timeNs).min
          if fastest <= 0 then "—"
          else
            val ratio = r.timeNs.toDouble / fastest
            if ratio <= 1.05 then "fastest" else f"$ratio%.1fx"

    var currentRanks: Map[(String, String, Int), Int] = Map.empty

    def rankStr(r: BenchmarkResult): String =
      if !r.isWarm then "—"
      else currentRanks.get((r.algoName, r.pattern, r.size)).map(n => s"#$n").getOrElse("—")

    def rankColor(r: BenchmarkResult): String =
      if !r.isWarm then Theme.TextDim
      else currentRanks.get((r.algoName, r.pattern, r.size)) match
        case Some(1) => Theme.AccentSuccess
        case Some(2) => Theme.AccentSecondary
        case Some(n) if n >= 7 => Theme.AccentDanger
        case _ => Theme.TextNormal

    def pctColor(r: BenchmarkResult): String =
      val pct = pctVsFastest(r)
      if pct == "fastest" then Theme.AccentSuccess
      else if pct == "—" then Theme.TextDim
      else
        val ratio = pct.dropRight(1).toDoubleOption.getOrElse(1.0)
        if ratio < 2.0 then Theme.AccentSecondary else Theme.AccentDanger

    // ── Table ─────────────────────────────────────────────────
    def makeCol(
                 title: String,
                 w: Double,
                 fn: BenchmarkResult => String,
                 colorFn: BenchmarkResult => String = _ => Theme.TextNormal
               ): TableColumn[BenchmarkResult, String] =
      val c = new TableColumn[BenchmarkResult, String](title)
      c.prefWidth = w
      c.cellValueFactory = cdf =>
        scalafx.beans.property.StringProperty(fn(cdf.value))
      c.delegate.setCellFactory { _ =>
        new javafx.scene.control.TableCell[BenchmarkResult, String]:
          override def updateItem(item: String, empty: Boolean): Unit =
            super.updateItem(item, empty)
            if empty || item == null then
              setText(null); setStyle("")
            else
              setText(item)
              val row = getTableRow
              if row != null && row.getItem != null then
                val color = colorFn(row.getItem)
                setStyle(
                  s"-fx-text-fill: $color; -fx-font-family: 'Consolas', monospace; " +
                    s"-fx-font-size: 11px; -fx-alignment: CENTER;"
                )
              else
                setStyle(
                  s"-fx-text-fill: ${Theme.TextNormal}; -fx-font-family: 'Consolas', monospace; " +
                    s"-fx-font-size: 11px; -fx-alignment: CENTER;"
                )
      }
      c

    val table = new TableView[BenchmarkResult](results)
    table.style = s"-fx-background-color: ${Theme.BgBase};"
    table.columnResizePolicy = TableView.ConstrainedResizePolicy
    table.delegate.getStylesheets.add(
      "data:text/css," +
        java.net.URLEncoder.encode(
          s""".table-view { -fx-background-color: ${Theme.BgBase}; }
             |.table-view .column-header-background { -fx-background-color: ${Theme.BgDeep}; }
             |.table-view .column-header {
             |  -fx-background-color: ${Theme.BgDeep};
             |  -fx-border-color: ${Theme.BgBorder}; -fx-border-width: 0 1 1 0;
             |}
             |.table-view .column-header .label {
             |  -fx-text-fill: ${Theme.TextDim}; -fx-font-family: 'Consolas', monospace;
             |  -fx-font-size: 10px; -fx-font-weight: bold;
             |}
             |.table-row-cell {
             |  -fx-background-color: ${Theme.BgBase};
             |  -fx-border-color: transparent transparent ${Theme.BgBorder} transparent;
             |  -fx-table-cell-border-color: transparent;
             |}
             |.table-row-cell:odd  { -fx-background-color: ${Theme.BgRaised}; }
             |.table-row-cell:selected { -fx-background-color: ${Theme.BgHover}; }
             |.table-row-cell:hover    { -fx-background-color: ${Theme.BgHover}; }
             |.table-cell {
             |  -fx-text-fill: ${Theme.TextNormal}; -fx-font-family: 'Consolas', monospace;
             |  -fx-font-size: 11px; -fx-border-color: transparent;
             |}""".stripMargin,
          "UTF-8"
        ).replace("+", "%20")
    )
    VBox.setVgrow(table, Priority.Always)

    table.columns ++= Seq(
      makeCol("Algorithm",  110, _.algoName,
        r => if r.hasFailure then Theme.AccentDanger
        else if r.isWarm then Theme.TextBright else Theme.TextDim),
      makeCol("Pattern",    100, _.pattern),
      makeCol("Size",        50, _.size.toString),
      makeCol("Run",         45,
        r => if r.hasFailure then "FAIL" else if r.isWarm then "WARM" else "COLD",
        r => if r.hasFailure then Theme.AccentDanger
        else if r.isWarm then Theme.AccentPrimary else Theme.TextDim),
      makeCol("Rank",        45, r => rankStr(r), r => rankColor(r)),
      makeCol("vs Fastest",  75, r => pctVsFastest(r), r => pctColor(r)),
      makeCol("Mean",        75, r => if r.timeMeanNs > 0 then f"${r.timeMeanNs/1e6}%.2f" else r.timeMsStr,
        r => if r.isWarm then Theme.TextBright else Theme.TextDim),
      makeCol("Median",      75, r => if r.timeMedianNs > 0 then f"${r.timeMedianNs/1e6}%.2f" else "—"),
      makeCol("P99",         70, r => if r.timeP99Ns > 0 then f"${r.timeP99Ns/1e6}%.2f" else "—",
        r => if r.timeP99Ns > r.timeMeanNs * 3 then Theme.AccentDanger else Theme.TextNormal),
      makeCol("StdDev",      70, r => if r.timeStdDevNs > 0 then f"${r.timeStdDevNs/1e6}%.2f" else "—"),
      makeCol("Throughput",  85, _.throughputStr),
      makeCol("Comparisons", 90, r => f"${r.comparisons}%,d"),
      makeCol("Swaps",       70, r => f"${r.swaps}%,d"),
      makeCol("Writes",      65, r => f"${r.writes}%,d"),
      makeCol("Heap MB",     70, r => f"${r.maxHeapMb}%.2f"),
      makeCol("CPU %",       60, r => f"${r.cpuPercent}%.1f"),
      makeCol("GC runs",     55, r => r.gcCollections.toString),
      makeCol("Sorted?",     60,
        r => if r.hasFailure then "FAIL" else if r.isSorted then "✓" else "✗",
        r => if r.hasFailure || !r.isSorted then Theme.AccentDanger else Theme.AccentSuccess),
      makeCol("Stable?",     60,
        r => if r.isStable then "✓" else "—",
        r => if r.isStable then Theme.AccentSuccess else Theme.TextDim)
    )

    results.onChange { (_, _) => Platform.runLater {
      currentRanks = rankMap()
      table.refresh()
    }}

    // ── Summary bar ───────────────────────────────────────────
    val summaryBox = new VBox(6)
    summaryBox.padding   = Insets(10, 14, 10, 14)
    summaryBox.minHeight = 70
    summaryBox.style     = s"-fx-background-color: ${Theme.BgBase}; " +
      s"-fx-border-color: ${Theme.BgBorder}; -fx-border-width: 1 0 0 0;"

    def updateSummary(): Unit =
      summaryBox.children.clear()
      val warm    = results.filter(r => r.isWarm && !r.hasFailure)
      val failed  = results.filter(_.hasFailure)
      if warm.isEmpty && failed.isEmpty then return

      if failed.nonEmpty then
        val failRow = new HBox(8)
        failRow.alignment = Pos.CenterLeft
        val failHdr = new Label("FAILURES")
        failHdr.style = Theme.titleStyle(9, Theme.AccentDanger)
        failRow.children.add(failHdr.delegate)
        failed.groupBy(_.algoName).foreach { (algo, _) =>
          failRow.children.add(chip(s"✗ $algo", Theme.AccentDanger))
        }
        summaryBox.children.add(failRow.delegate)

      if warm.nonEmpty then
        val fastest    = warm.minBy(_.timeNs)
        val slowest    = warm.maxBy(_.timeNs)
        val leastComps = warm.minBy(_.comparisons)
        val mostSwaps  = warm.maxBy(_.swaps)
        val highP99    = warm.filter(_.timeP99Ns > 0).maxByOption(_.timeP99Ns)

        val row1 = new HBox(10)
        row1.alignment = Pos.CenterLeft
        val sumHdr = new Label("SUMMARY"); sumHdr.style = Theme.titleStyle(9)
        row1.children.addAll(
          sumHdr.delegate,
          chip(s"Fastest: ${fastest.algoName}/${fastest.pattern} (${fastest.timeMsStr})", Theme.AccentSuccess),
          chip(s"Slowest: ${slowest.algoName}/${slowest.pattern} (${slowest.timeMsStr})", Theme.AccentDanger),
          chip(s"Fewest cmps: ${leastComps.algoName} (${f"${leastComps.comparisons}%,d"})", Theme.AccentPrimary),
          chip(s"Most swaps: ${mostSwaps.algoName} (${f"${mostSwaps.swaps}%,d"})", Theme.AccentSecondary)
        )
        highP99.foreach { hp =>
          row1.children.add(chip(s"High P99: ${hp.algoName} (${f"${hp.timeP99Ns/1e6}%.1f"}ms)", Theme.AccentDanger))
        }
        summaryBox.children.add(row1.delegate)

        val speedups = results.filter(!_.hasFailure).groupBy(_.algoName).flatMap { (name, runs) =>
          val cold     = runs.find(!_.isWarm)
          val warmRuns = runs.filter(_.isWarm)
          if cold.isDefined && warmRuns.nonEmpty then
            val avgWarm = warmRuns.map(_.timeNs).sum / warmRuns.size
            Some(name -> cold.get.timeNs.toDouble / avgWarm)
          else None
        }.toSeq.sortBy(-_._2)

        if speedups.nonEmpty then
          val row2 = new HBox(8)
          row2.alignment = Pos.CenterLeft
          val jitHdr = new Label("JIT SPEEDUP"); jitHdr.style = Theme.titleStyle(9)
          row2.children.add(jitHdr.delegate)
          speedups.foreach { (name, ratio) =>
            val color = if ratio > 5 then Theme.AccentSuccess
            else if ratio > 2 then Theme.AccentSecondary
            else Theme.TextDim
            row2.children.add(chip(f"$name: $ratio%.1fx", color))
          }
          summaryBox.children.add(row2.delegate)

    results.onChange { (_, _) => Platform.runLater { updateSummary() } }

    // ── Config ────────────────────────────────────────────────
    val algoChecks: Map[AlgorithmType, CheckBox] =
      AlgorithmType.values.map { a =>
        val cb = new CheckBox(a.label)
        cb.selected = true
        cb.style = Theme.labelStyle(11, Theme.TextNormal)
        a -> cb
      }.toMap

    val genChecks: Map[GeneratorType, CheckBox] =
      GeneratorType.values.map { g =>
        val cb = new CheckBox(g.label)
        cb.selected = Seq(
          GeneratorType.Random, GeneratorType.Sorted,
          GeneratorType.SortedReverse, GeneratorType.NearlySorted
        ).contains(g)
        cb.style = Theme.labelStyle(11, Theme.TextNormal)
        g -> cb
      }.toMap

    val sizeChecks: Map[Int, CheckBox] =
      Seq(100, 500, 1000, 5000, 10000, 100000, 1000000).map { n =>
        val cb = new CheckBox(n.toString)
        cb.selected = n <= 1000
        cb.style = Theme.labelStyle(11, Theme.TextNormal)
        n -> cb
      }.toMap

    val warmupCheck = new CheckBox("JIT Warmup (5000 throwaway runs)")
    warmupCheck.selected = true
    warmupCheck.style = Theme.labelStyle(11, Theme.TextNormal)

    val validateFirstCheck = new CheckBox("Validate correctness before run")
    validateFirstCheck.selected = true
    validateFirstCheck.style = Theme.labelStyle(11, Theme.TextNormal)

    // ── Progress ──────────────────────────────────────────────
    val progressBar = new ProgressBar
    progressBar.progress = 0.0
    progressBar.prefWidth = Double.MaxValue
    progressBar.maxWidth  = Double.MaxValue
    progressBar.style     = s"-fx-accent: ${Theme.AccentPrimary};"
    progressBar.visible   = false

    val progressLbl = new Label("Configure options and click RUN")
    progressLbl.style = Theme.labelStyle(10, Theme.TextDim)

    val btnRun = new Button("▶  RUN BENCHMARK")
    btnRun.style    = Theme.buttonPrimary
    btnRun.maxWidth = Double.MaxValue

    val btnClear = new Button("✕  CLEAR")
    btnClear.style    = Theme.buttonSecondary
    btnClear.maxWidth = Double.MaxValue

    val btnExportCsv = new Button("⬇  EXCEL")
    btnExportCsv.style    = Theme.buttonSecondary
    btnExportCsv.maxWidth = Double.MaxValue
    btnExportCsv.disable  = true

    val btnExportJson = new Button("⬇  JSON")
    btnExportJson.style    = Theme.buttonSecondary
    btnExportJson.maxWidth = Double.MaxValue
    btnExportJson.disable  = true

    val btnValidate = new Button("✓  VALIDATE ONLY")
    btnValidate.style    = Theme.buttonSecondary
    btnValidate.maxWidth = Double.MaxValue

    results.onChange { (_, _) =>
      val hasData = results.nonEmpty
      btnExportCsv.disable  = !hasData
      btnExportJson.disable = !hasData
    }

    btnClear.onAction = _ =>
      results.clear()
      progressLbl.text     = "Results cleared"
      progressBar.progress = 0.0

    btnExportCsv.onAction = _ =>
      Future {
        val path = BenchmarkExporter.exportExcel(results.toSeq)
        Platform.runLater { progressLbl.text = s"Excel exported: $path" }
      }

    btnExportJson.onAction = _ =>
      Future {
        val path = BenchmarkExporter.exportJson(results.toSeq)
        Platform.runLater { progressLbl.text = s"JSON exported: $path" }
      }

    btnValidate.onAction = _ =>
      btnValidate.disable = true
      progressLbl.text    = "Running correctness validation…"
      Future {
        val validResults = CorrectnessValidator.validateAll()
        val passed       = validResults.count(_.passed)
        val failed       = validResults.filterNot(_.passed)
        Platform.runLater {
          btnValidate.disable = false
          if failed.isEmpty then
            progressLbl.text  = s"✓ All $passed checks passed"
            progressLbl.style = Theme.labelStyle(10, Theme.AccentSuccess)
          else
            progressLbl.text  = s"✗ ${failed.size} checks failed / $passed passed"
            progressLbl.style = Theme.labelStyle(10, Theme.AccentDanger)
            // Add failure entries to results table so user can see them
            failed.foreach { f =>
              results += BenchmarkResult(
                algoName   = f.algoName,
                variant    = "Validation",
                pattern    = f.pattern,
                size       = f.size,
                isWarm     = false,
                timeNs     = 0L,
                failureMsg = f.message
              )
            }
        }
      }

    btnRun.onAction = _ =>
      val algos    = algoChecks.filter(_._2.selected.value).keys.toList
      val gens     = genChecks.filter(_._2.selected.value).keys.toList
      val sizes    = sizeChecks.filter(_._2.selected.value).keys.toList.sorted
      val doWarmup = warmupCheck.selected.value
      if algos.isEmpty || gens.isEmpty || sizes.isEmpty then
        progressLbl.text = "Select at least one algorithm, pattern, and size"
      else
        btnRun.disable       = true
        btnClear.disable     = true
        progressBar.visible  = true
        progressBar.progress = 0.0
        val total = algos.size * gens.size * sizes.size
        var done  = 0
        Future {
          // ── Global warmup phase: warm up all algorithms first ────
          if doWarmup then
            for
              size <- sizes
              gen  <- gens
            do
              BenchmarkRunner.globalWarmup(algos, gen, size, msg => Platform.runLater { progressLbl.text = msg })
              System.gc()
              Thread.sleep(100)

          // ── Measurement phase: run all measurements ────
          for
            size <- sizes
            gen  <- gens
            algo <- algos
          do
            val runs = BenchmarkRunner.run(
              BenchmarkRunner.RunConfig(algo, gen, size, doWarmup, skipColdRun = doWarmup),
              msg => Platform.runLater { progressLbl.text = msg }
            )
            done += 1
            val snapshot = done
            Platform.runLater {
              results.addAll(runs*)
              progressBar.progress = snapshot.toDouble / total
              progressLbl.text = s"$snapshot / $total combinations complete"
            }
          Platform.runLater {
            btnRun.disable      = false
            btnClear.disable    = false
            progressBar.visible = false
            progressLbl.text    = s"Done — ${results.size} data points"
          }
        }

    def quickBtn(label: String, color: String, action: => Unit): Button =
      val b = new Button(label)
      b.style = s"-fx-background-color: transparent; -fx-text-fill: $color; " +
        s"-fx-font-size: 9px; -fx-font-family: 'Consolas', monospace; " +
        s"-fx-cursor: hand; -fx-padding: 0 4;"
      b.onAction = _ => action
      b

    def section(title: String, checks: Iterable[CheckBox]): VBox =
      val titleLbl = hdr(title)
      val sep      = new Label("|"); sep.style = Theme.labelStyle(9, Theme.TextDim)
      val titleRow = new HBox(4)
      titleRow.alignment = Pos.CenterLeft
      titleRow.children.addAll(
        titleLbl.delegate,
        quickBtn("all",  Theme.AccentPrimary, checks.foreach(_.selected = true)).delegate,
        sep.delegate,
        quickBtn("none", Theme.TextDim, checks.foreach(_.selected = false)).delegate
      )
      val flow = new FlowPane(6, 5)
      flow.padding = Insets(4, 0, 0, 0)
      checks.foreach(cb => flow.children.add(cb.delegate))
      val box = new VBox(3)
      box.children.addAll(titleRow.delegate, flow.delegate)
      box

    // ── Export row ────────────────────────────────────────────
    val exportRow = new HBox(4)
    HBox.setHgrow(btnExportCsv,  Priority.Always)
    HBox.setHgrow(btnExportJson, Priority.Always)
    exportRow.children.addAll(btnExportCsv.delegate, btnExportJson.delegate)

    val vGrow = new Region
    VBox.setVgrow(vGrow, Priority.Always)

    val configPanel = new VBox(0)
    configPanel.prefWidth = 260
    configPanel.minWidth  = 260
    configPanel.maxWidth  = 260
    configPanel.style   = s"-fx-background-color: ${Theme.BgBase}; " +
      s"-fx-border-color: ${Theme.BgBorder}; -fx-border-width: 0 1 0 0;"
    configPanel.padding = Insets(14, 12, 14, 12)
    configPanel.children.addAll(
      section("ALGORITHMS",    algoChecks.values).delegate,
      spacer(10),
      section("DATA PATTERNS", genChecks.values).delegate,
      spacer(10),
      section("ARRAY SIZES",   sizeChecks.values.toSeq.sortBy(_.text.value.toInt)).delegate,
      spacer(10),
      hdr("OPTIONS").delegate,
      spacer(4),
      warmupCheck.delegate,
      spacer(3),
      validateFirstCheck.delegate,
      vGrow,
      divider(),
      progressBar.delegate,
      spacer(4),
      progressLbl.delegate,
      spacer(8),
      btnRun.delegate,
      spacer(4),
      btnValidate.delegate,
      spacer(4),
      btnClear.delegate,
      spacer(4),
      exportRow.delegate
    )

    val tableBox = new VBox(0)
    tableBox.style = s"-fx-background-color: ${Theme.BgDeep};"
    VBox.setVgrow(table, Priority.Always)
    HBox.setHgrow(tableBox, Priority.Always)
    tableBox.children.addAll(table.delegate, summaryBox.delegate)

    val runPage = new HBox
    runPage.style = s"-fx-background-color: ${Theme.BgDeep};"
    VBox.setVgrow(runPage, Priority.Always)
    runPage.children.addAll(configPanel.delegate, tableBox.delegate)

    val analysisPage = BenchmarkAnalysisPage.build(results)

    val pageArea = new StackPane
    pageArea.style = s"-fx-background-color: ${Theme.BgDeep};"
    VBox.setVgrow(pageArea, Priority.Always)

    def makeSwitch(label: String): (VBox, Label, Region) =
      val lbl = new Label(label)
      lbl.style   = Theme.labelStyle(11, Theme.TextDim)
      lbl.padding = Insets(0, 6, 0, 6)
      val ind = new Region
      ind.prefHeight = 2; ind.maxWidth = Double.MaxValue
      ind.style = "-fx-background-color: transparent;"
      val tab = new VBox(0)
      tab.alignment = Pos.Center; tab.prefHeight = 36
      tab.padding   = Insets(0, 14, 0, 14)
      tab.style     = "-fx-cursor: hand;"
      tab.children.addAll(lbl.delegate, ind)
      (tab, lbl, ind)

    val (runTab,      runLbl,      runInd)      = makeSwitch("⚙  RUN")
    val (analysisTab, analysisLbl, analysisInd) = makeSwitch("📈  ANALYSIS")

    def activateSwitch(
                        activeTab: VBox, activeLbl: Label, activeInd: Region,
                        inactiveTab: VBox, inactiveLbl: Label, inactiveInd: Region
                      ): Unit =
      activeLbl.style    = Theme.labelStyle(11, Theme.AccentPrimary)
      activeInd.style    = s"-fx-background-color: ${Theme.AccentPrimary};"
      activeTab.style    = s"-fx-cursor: hand; -fx-background-color: ${Theme.BgBase};"
      inactiveLbl.style  = Theme.labelStyle(11, Theme.TextDim)
      inactiveInd.style  = "-fx-background-color: transparent;"
      inactiveTab.style  = "-fx-cursor: hand;"

    def showRun(): Unit =
      pageArea.children.clear()
      pageArea.children.add(runPage.delegate)
      activateSwitch(runTab, runLbl, runInd, analysisTab, analysisLbl, analysisInd)

    def showAnalysis(): Unit =
      pageArea.children.clear()
      pageArea.children.add(analysisPage.delegate)
      activateSwitch(analysisTab, analysisLbl, analysisInd, runTab, runLbl, runInd)

    runTab.delegate.setOnMouseClicked(_ => showRun())
    analysisTab.delegate.setOnMouseClicked(_ => showAnalysis())

    val switchBar = new HBox(0)
    switchBar.prefHeight = 36
    switchBar.alignment  = Pos.CenterLeft
    switchBar.padding    = Insets(0, 0, 0, 8)
    switchBar.style      = s"-fx-background-color: ${Theme.BgBase}; " +
      s"-fx-border-color: ${Theme.BgBorder}; -fx-border-width: 0 0 1 0;"
    switchBar.children.addAll(runTab.delegate, analysisTab.delegate)

    showRun()

    val page = new BorderPane
    page.style  = s"-fx-background-color: ${Theme.BgDeep};"
    page.top    = switchBar
    page.center = pageArea
    page
