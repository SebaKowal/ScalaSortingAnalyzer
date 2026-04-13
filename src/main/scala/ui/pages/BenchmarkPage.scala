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

    def divider(): Region =
      val r = new Region
      r.prefHeight = 1
      r.maxWidth   = Double.MaxValue
      r.style = s"-fx-background-color: ${Theme.BgBorder};"
      VBox.setMargin(r, Insets(10, 0, 10, 0))
      r

    def chip(text: String, color: String): Label =
      val l = new Label(text)
      l.style = s"-fx-background-color: ${color}22; -fx-text-fill: $color; " +
        s"-fx-padding: 2 7 2 7; -fx-background-radius: 3; " +
        s"-fx-font-family: 'Consolas', monospace; -fx-font-size: 10px;"
      l

    // ── Results ───────────────────────────────────────────────
    val results = ObservableBuffer.empty[BenchmarkResult]

    // ── Rank helpers ──────────────────────────────────────────
    // Group warm results by (size, pattern), rank by timeNs ascending
    def rankMap(): Map[(String, String, Int), Int] =
      val warm = results.filter(_.isWarm)
      val grouped = warm.groupBy(r => (r.pattern, r.algoName, r.size))
      // average timeNs per (algo, pattern, size) group
      val avgTimes = grouped.map { (k, runs) =>
        k -> runs.map(_.timeNs).sum / runs.size
      }
      // for each (pattern, size) bucket, rank algos by avg time
      val byBucket = avgTimes.groupBy { case ((pattern, _, size), _) => (pattern, size) }
      byBucket.flatMap { case ((pattern, size), entries) =>
        val sorted = entries.toSeq.sortBy(_._2)
        sorted.zipWithIndex.map { case (((p, algo, s), _), idx) =>
          (algo, p, s) -> (idx + 1)
        }
      }.toMap

    def pctVsFastest(r: BenchmarkResult, ranks: Map[(String, String, Int), Int]): String =
      if !r.isWarm then "—"
      else
        val warm = results.filter(w => w.isWarm && w.pattern == r.pattern && w.size == r.size)
        if warm.isEmpty then "—"
        else
          val fastest = warm.map(_.timeNs).min
          if fastest <= 0 then "—"
          else
            val ratio = r.timeNs.toDouble / fastest
            if ratio <= 1.05 then "fastest"
            else f"${ratio}%.1fx"

    def rankStr(r: BenchmarkResult, ranks: Map[(String, String, Int), Int]): String =
      if !r.isWarm then "—"
      else ranks.get((r.algoName, r.pattern, r.size)).map(n => s"#$n").getOrElse("—")

    def rankColor(r: BenchmarkResult, ranks: Map[(String, String, Int), Int]): String =
      if !r.isWarm then Theme.TextDim
      else ranks.get((r.algoName, r.pattern, r.size)) match
        case Some(1) => Theme.AccentSuccess
        case Some(2) => Theme.AccentSecondary
        case Some(n) if n >= 7 => Theme.AccentDanger
        case _ => Theme.TextNormal

    // ── Table ─────────────────────────────────────────────────
    def strCol(
                 title: String,
                 w: Double,
                 fn: BenchmarkResult => String,
                 colorFn: BenchmarkResult => String = _ => Theme.TextNormal
               ): TableColumn[BenchmarkResult, String] =
      val c = new TableColumn[BenchmarkResult, String](title)
      c.prefWidth = w
      c.cellValueFactory = cdf =>
        scalafx.beans.property.StringProperty(fn(cdf.value))
      // Use javafx.scene.control.TableCell directly to avoid ScalaFX member conflicts
      c.delegate.setCellFactory { _ =>
        new javafx.scene.control.TableCell[BenchmarkResult, String]:
          override def updateItem(item: String, empty: Boolean): Unit =
            super.updateItem(item, empty)
            if empty || item == null then
              setText(null)
              setStyle("")
            else
              setText(item)
              val row = getTableRow
              if row != null && row.getItem != null then
                val r = row.getItem
                val color = colorFn(r)
                setStyle(
                  s"-fx-text-fill: $color; " +
                    s"-fx-font-family: 'Consolas', monospace; " +
                    s"-fx-font-size: 11px; -fx-alignment: CENTER;"
                )
              else
                setStyle(
                  s"-fx-text-fill: ${Theme.TextNormal}; " +
                    s"-fx-font-family: 'Consolas', monospace; " +
                    s"-fx-font-size: 11px; -fx-alignment: CENTER;"
                )
      }
      c

    val table = new TableView[BenchmarkResult](results)
    table.style = s"-fx-background-color: ${Theme.BgBase}; -fx-text-fill: ${Theme.TextBright};"
    table.columnResizePolicy = TableView.ConstrainedResizePolicy
    table.delegate.getStylesheets.add(Theme.tableViewStylesheet)

    VBox.setVgrow(table, Priority.Always)

    // Rebuild columns using current rank data each time results change
    def rebuildColumns(): Unit =
      val ranks = rankMap()
      table.columns.clear()
      table.columns ++= Seq(
        strCol("Algorithm", 110, _.algoName,
          r => if r.isWarm then Theme.TextBright else Theme.TextDim),
        strCol("Pattern", 100, _.pattern),
        strCol("Size", 50, _.size.toString),
        strCol("Run", 45,
          r => if r.isWarm then "WARM" else "COLD",
          r => if r.isWarm then Theme.AccentPrimary else Theme.TextDim),
        strCol("Rank", 45,
          r => rankStr(r, ranks),
          r => rankColor(r, ranks)),
        strCol("vs Fastest", 75,
          r => pctVsFastest(r, ranks),
          r =>
            val pct = pctVsFastest(r, ranks)
            if pct == "fastest" then Theme.AccentSuccess
            else if pct == "—" then Theme.TextDim
            else
              val ratio = pct.dropRight(1).toDoubleOption.getOrElse(1.0)
              if ratio < 1.5 then Theme.AccentSecondary
              else if ratio < 3.0 then Theme.AccentSecondary
              else Theme.AccentDanger
        ),
        strCol("Time", 75, _.timeMsStr,
          r => if r.isWarm then Theme.TextBright else Theme.TextDim),
        strCol("Throughput", 85, _.throughputStr),
        strCol("Comparisons", 90, r => f"${r.comparisons}%,d"),
        strCol("Swaps", 70, r => f"${r.swaps}%,d"),
        strCol("Writes", 65, r => f"${r.writes}%,d"),
        strCol("Heap Δ MB", 70, r => f"${r.heapDeltaMb}%.2f"),
        strCol("GC runs", 55, r => r.gcCollections.toString),
        strCol("Sorted?", 55,
          r => if r.isSorted then "✓" else "✗",
          r => if r.isSorted then Theme.AccentSuccess else Theme.AccentDanger),
        strCol("Stable?", 55,
          r => if r.isStable then "✓" else "—",
          r => if r.isStable then Theme.AccentSuccess else Theme.TextDim)
      )

    rebuildColumns()

    results.onChange { (_, _) =>
      Platform.runLater { rebuildColumns() }
    }

    // ── Summary bar ───────────────────────────────────────────
    val summaryBox = new VBox(6)
    summaryBox.padding  = Insets(10, 14, 10, 14)
    summaryBox.minHeight = 70
    summaryBox.style    = s"-fx-background-color: ${Theme.BgBase}; " +
      s"-fx-border-color: ${Theme.BgBorder}; -fx-border-width: 1 0 0 0;"

    def updateSummary(): Unit =
      summaryBox.children.clear()
      val warm = results.filter(_.isWarm)
      if warm.isEmpty then return

      val fastest    = warm.minBy(_.timeNs)
      val slowest    = warm.maxBy(_.timeNs)
      val leastComps = warm.minBy(_.comparisons)
      val mostSwaps  = warm.maxBy(_.swaps)

      val row1 = new HBox(10)
      row1.alignment = Pos.CenterLeft
      val sumHdr = new Label("SUMMARY")
      sumHdr.style = Theme.titleStyle(9)
      row1.children.addAll(
        sumHdr.delegate,
        chip(s"⚡ Fastest: ${fastest.algoName} / ${fastest.pattern} (${fastest.timeMsStr})", Theme.AccentSuccess),
        chip(s"🐢 Slowest: ${slowest.algoName} / ${slowest.pattern} (${slowest.timeMsStr})", Theme.AccentDanger),
        chip(s"🔍 Fewest comparisons: ${leastComps.algoName} (${f"${leastComps.comparisons}%,d"})", Theme.AccentPrimary),
        chip(s"🔄 Most swaps: ${mostSwaps.algoName} (${f"${mostSwaps.swaps}%,d"})", Theme.AccentSecondary)
      )
      summaryBox.children.add(row1.delegate)

      // JIT speedup row
      val speedups = results.groupBy(_.algoName).flatMap { (name, runs) =>
        val cold     = runs.find(!_.isWarm)
        val warmRuns = runs.filter(_.isWarm)
        if cold.isDefined && warmRuns.nonEmpty then
          val avgWarm = warmRuns.map(_.timeNs).sum / warmRuns.size
          val ratio   = cold.get.timeNs.toDouble / avgWarm
          Some(name -> ratio)
        else None
      }.toSeq.sortBy(-_._2)

      if speedups.nonEmpty then
        val row2 = new HBox(8)
        row2.alignment = Pos.CenterLeft
        val jitHdr = new Label("JIT SPEEDUP")
        jitHdr.style = Theme.titleStyle(9)
        row2.children.add(jitHdr.delegate)
        speedups.foreach { (name, ratio) =>
          val color =
            if ratio > 5  then Theme.AccentSuccess
            else if ratio > 2 then Theme.AccentSecondary
            else Theme.TextDim
          row2.children.add(chip(f"$name: ${ratio}%.1fx", color))
        }
        summaryBox.children.add(row2.delegate)

    results.onChange { (_, _) =>
      Platform.runLater { updateSummary() }
    }

    // ── Config checkboxes ─────────────────────────────────────
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
          GeneratorType.Random,
          GeneratorType.Sorted,
          GeneratorType.SortedReverse,
          GeneratorType.NearlySorted
        ).contains(g)
        cb.style = Theme.labelStyle(11, Theme.TextNormal)
        g -> cb
      }.toMap

    val sizeChecks: Map[Int, CheckBox] =
      Seq(100, 500, 1000, 5000, 10000).map { n =>
        val cb = new CheckBox(n.toString)
        cb.selected = n <= 1000
        cb.style = Theme.labelStyle(11, Theme.TextNormal)
        n -> cb
      }.toMap

    val warmupCheck = new CheckBox("JIT Warmup  (500 throwaway runs)")
    warmupCheck.selected = true
    warmupCheck.style = Theme.labelStyle(11, Theme.TextNormal)

    val warmOnlyCheck = new CheckBox("Show warm runs only in table")
    warmOnlyCheck.selected = false
    warmOnlyCheck.style = Theme.labelStyle(11, Theme.TextNormal)

    warmOnlyCheck.selected.onChange { (_, _, warmOnly) =>
      // filter table view — we keep all data, just change what's shown
      // rebuild with filter applied
      Platform.runLater { rebuildColumns() }
    }

    // ── Progress ──────────────────────────────────────────────
    val progressBar = new ProgressBar
    progressBar.progress = 0.0
    progressBar.prefWidth = Double.MaxValue
    progressBar.maxWidth  = Double.MaxValue
    progressBar.style     = s"-fx-accent: ${Theme.AccentPrimary};"
    progressBar.visible   = false

    val progressLbl = new Label("Configure options and click RUN")
    progressLbl.style = Theme.labelStyle(10, Theme.TextDim)

    // ── Run / clear buttons ───────────────────────────────────
    val btnRun = new Button("▶  RUN BENCHMARK")
    btnRun.style    = Theme.buttonPrimary
    btnRun.maxWidth = Double.MaxValue

    val btnClear = new Button("✕  CLEAR")
    btnClear.style    = Theme.buttonSecondary
    btnClear.maxWidth = Double.MaxValue

    btnClear.onAction = _ =>
      results.clear()
      progressLbl.text     = "Results cleared"
      progressBar.progress = 0.0

    btnRun.onAction = _ =>
      val algos    = algoChecks.filter(_._2.selected.value).keys.toList
      val gens     = genChecks.filter(_._2.selected.value).keys.toList
      val sizes    = sizeChecks.filter(_._2.selected.value).keys.toList.sorted
      val doWarmup = warmupCheck.selected.value

      if algos.isEmpty || gens.isEmpty || sizes.isEmpty then
        progressLbl.text = "⚠  Select at least one algorithm, pattern, and size"
      else
        btnRun.disable   = true
        btnClear.disable = true
        progressBar.visible  = true
        progressBar.progress = 0.0

        val total = algos.size * gens.size * sizes.size
        var done  = 0

        Future {
          for
            size <- sizes
            algo <- algos
            gen  <- gens
          do
            val config = BenchmarkRunner.RunConfig(
              algo      = algo,
              generator = gen,
              size      = size,
              warmup    = doWarmup
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
            btnRun.disable    = false
            btnClear.disable  = false
            progressBar.visible = false
            progressLbl.text  = s"✓  Done — ${results.size} data points collected"
          }
        }

    // ── Select all / none helpers ─────────────────────────────
    def selectAllBtn(checks: Iterable[CheckBox]): Button =
      val b = new Button("all")
      b.style = s"-fx-background-color: transparent; -fx-text-fill: ${Theme.AccentPrimary}; " +
        s"-fx-font-size: 9px; -fx-font-family: 'Consolas', monospace; -fx-cursor: hand; -fx-padding: 0 4;"
      b.onAction = _ => checks.foreach(_.selected = true)
      b

    def selectNoneBtn(checks: Iterable[CheckBox]): Button =
      val b = new Button("none")
      b.style = s"-fx-background-color: transparent; -fx-text-fill: ${Theme.TextDim}; " +
        s"-fx-font-size: 9px; -fx-font-family: 'Consolas', monospace; -fx-cursor: hand; -fx-padding: 0 4;"
      b.onAction = _ => checks.foreach(_.selected = false)
      b

    def sectionWithControls(title: String, checks: Iterable[CheckBox]): VBox =
      val titleLbl = hdr(title)
      val allBtn  = selectAllBtn(checks)
      val noneBtn = selectNoneBtn(checks)
      val sep = new Label("|")
      sep.style = Theme.labelStyle(9, Theme.TextDim)
      val titleRow = new HBox(4)
      titleRow.alignment = Pos.CenterLeft
      titleRow.children.addAll(titleLbl.delegate, allBtn.delegate, sep.delegate, noneBtn.delegate)

      val flow = new FlowPane(6, 5)
      flow.padding = Insets(4, 0, 0, 0)
      checks.foreach(cb => flow.children.add(cb.delegate))

      val box = new VBox(3)
      box.children.addAll(titleRow.delegate, flow.delegate)
      box

    // ── Config panel ──────────────────────────────────────────
    val algoSection = sectionWithControls("ALGORITHMS", algoChecks.values)
    val genSection  = sectionWithControls("DATA PATTERNS", genChecks.values)
    val sizeSection = sectionWithControls("ARRAY SIZES", sizeChecks.values.toSeq.sortBy(_.text.value.toInt))

    val optBox = new VBox(5)
    optBox.children.addAll(
      warmupCheck.delegate,
      warmOnlyCheck.delegate
    )

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
      algoSection.delegate,
      spacer(10),
      genSection.delegate,
      spacer(10),
      sizeSection.delegate,
      spacer(10),
      hdr("OPTIONS").delegate,
      spacer(4),
      optBox.delegate,
      vGrow,
      divider(),
      progressBar.delegate,
      spacer(4),
      progressLbl.delegate,
      spacer(8),
      btnRun.delegate,
      spacer(4),
      btnClear.delegate
    )

    // ── Table + summary box ───────────────────────────────────
    val tableBox = new VBox(0)
    tableBox.style = s"-fx-background-color: ${Theme.BgDeep};"
    VBox.setVgrow(table, Priority.Always)
    HBox.setHgrow(tableBox, Priority.Always)
    tableBox.children.addAll(table.delegate, summaryBox.delegate)

    val mainRow = new HBox
    VBox.setVgrow(mainRow, Priority.Always)
    mainRow.children.addAll(configPanel.delegate, tableBox.delegate)

    val page = new BorderPane
    page.style  = s"-fx-background-color: ${Theme.BgDeep};"
    page.center = mainRow
    page