package ui.pages

import benchmark.BenchmarkResult
import ui.Theme
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Label, ScrollPane}
import scalafx.scene.layout.*
import scalafx.Includes.*
import scalafx.application.Platform
import javafx.scene.chart.{LineChart, BarChart, XYChart, NumberAxis, CategoryAxis}
import javafx.scene.layout.GridPane

object BenchmarkAnalysisPage:

  def build(results: ObservableBuffer[BenchmarkResult]): BorderPane =

    def spacer(h: Int = 8): Region =
      val r = new Region; r.prefHeight = h; r

    // ── Mini nav ──────────────────────────────────────────────
    enum AnalysisTab:
      case Scaling, Comparisons, Heatmap, Insights

    var currentTab = AnalysisTab.Scaling

    val contentArea = new StackPane
    contentArea.style = s"-fx-background-color: ${Theme.BgDeep};"
    VBox.setVgrow(contentArea, Priority.Always)
    HBox.setHgrow(contentArea, Priority.Always)

    val miniNav = new HBox(0)
    miniNav.style     = s"-fx-background-color: ${Theme.BgDeep}; " +
      s"-fx-border-color: ${Theme.BgBorder}; -fx-border-width: 0 0 1 0;"
    miniNav.prefHeight = 38
    miniNav.alignment  = Pos.CenterLeft
    miniNav.padding    = Insets(0, 0, 0, 8)

    def miniNavItem(label: String, tab: AnalysisTab, showFn: () => Unit): VBox =
      val lbl = new Label(label)
      lbl.style   = Theme.labelStyle(11, Theme.TextDim)
      lbl.padding = Insets(0, 4, 0, 4)
      val indicator = new Region
      indicator.prefHeight = 2
      indicator.maxWidth   = Double.MaxValue
      indicator.style      = "-fx-background-color: transparent;"
      val item = new VBox(0)
      item.alignment  = Pos.Center
      item.padding    = Insets(0, 14, 0, 14)
      item.prefHeight = 38
      item.children.addAll(lbl.delegate, indicator)
      item.style = "-fx-cursor: hand;"

      def setActive(active: Boolean): Unit =
        if active then
          lbl.style       = Theme.labelStyle(11, Theme.AccentPrimary)
          indicator.style = s"-fx-background-color: ${Theme.AccentPrimary};"
          item.style      = s"-fx-cursor: hand; -fx-background-color: ${Theme.BgBase};"
        else
          lbl.style       = Theme.labelStyle(11, Theme.TextDim)
          indicator.style = "-fx-background-color: transparent;"
          item.style      = "-fx-cursor: hand;"

      setActive(currentTab == tab)
      item.delegate.setOnMouseClicked(_ =>
        currentTab = tab
        miniNav.children.forEach { node =>
          node.setStyle(s"-fx-cursor: hand;")
        }
        setActive(true)
        showFn()
      )
      item.delegate.setOnMouseEntered(_ =>
        if currentTab != tab then
          item.style = s"-fx-cursor: hand; -fx-background-color: ${Theme.BgRaised};"
      )
      item.delegate.setOnMouseExited(_ => setActive(currentTab == tab))
      item

    // ── Shared helpers ────────────────────────────────────────
    case class AvgResult(
                          algoName: String, pattern: String, size: Int,
                          avgTimeMs: Double, avgComparisons: Double, avgSwaps: Double,
                          avgHeapDeltaMb: Double, avgGcCollections: Double
                        )

    def aggregate(warm: Seq[BenchmarkResult]): Seq[AvgResult] =
      warm.groupBy(r => (r.algoName, r.pattern, r.size)).map {
        case ((algo, pat, sz), rs) =>
          AvgResult(
            algo, pat, sz,
            avgTimeMs        = rs.map(_.timeMs).sum / rs.size,
            avgComparisons   = rs.map(_.comparisons.toDouble).sum / rs.size,
            avgSwaps         = rs.map(_.swaps.toDouble).sum / rs.size,
            avgHeapDeltaMb   = rs.map(_.maxHeapMb).sum / rs.size,
            avgGcCollections = rs.map(_.gcCollections.toDouble).sum / rs.size
          )
      }.toSeq

    val palette = Seq(
      "#00d4ff", "#ff8c00", "#00ff9d", "#ff2d6b",
      "#a855f7", "#facc15", "#38bdf8", "#f472b6"
    )

    def seriesColor(idx: Int): String = palette(idx % palette.size)

    def applyChartCss(chart: javafx.scene.chart.XYChart[?, ?], colors: Seq[String]): Unit =
      val css = colors.zipWithIndex.map { (color, idx) =>
        s""".default-color$idx.chart-series-line { -fx-stroke: $color; -fx-stroke-width: 2; }
           |.default-color$idx.chart-line-symbol { -fx-background-color: $color, white; }
           |.default-color$idx.chart-bar { -fx-bar-fill: $color; }""".stripMargin
      }.mkString("\n")
      val encoded = java.net.URLEncoder.encode(css, "UTF-8").replace("+", "%20")
      chart.getStylesheets.add(s"data:text/css,$encoded")

    // Base chart CSS for dark theme
    val darkChartCss =
      s""".chart { -fx-background-color: ${Theme.BgDeep}; -fx-padding: 8; }
         |.chart-plot-background { -fx-background-color: ${Theme.BgBase}; }
         |.chart-vertical-grid-lines { -fx-stroke: ${Theme.BgBorder}; }
         |.chart-horizontal-grid-lines { -fx-stroke: ${Theme.BgBorder}; }
         |.chart-alternative-row-fill { -fx-fill: transparent; }
         |.chart-legend { -fx-background-color: ${Theme.BgRaised};
         |  -fx-text-fill: ${Theme.TextNormal}; }
         |.axis { -fx-tick-label-fill: ${Theme.TextNormal};
         |  -fx-font-family: 'Consolas', monospace; -fx-font-size: 10px; }
         |.axis-label { -fx-text-fill: ${Theme.TextDim};
         |  -fx-font-family: 'Consolas', monospace; -fx-font-size: 11px; }
         |.chart-legend-item { -fx-text-fill: ${Theme.TextNormal};
         |  -fx-font-family: 'Consolas', monospace; -fx-font-size: 10px; }""".stripMargin

    def darkChartStylesheet: String =
      "data:text/css," + java.net.URLEncoder.encode(darkChartCss, "UTF-8").replace("+", "%20")

    def interpolateColor(fraction: Double): String =
      val f = fraction.max(0.0).min(1.0)
      val (r1, g1, b1, r2, g2, b2) =
        if f <= 0.5 then (0x00, 0xff, 0x9d, 0xff, 0x8c, 0x00)
        else             (0xff, 0x8c, 0x00, 0xff, 0x2d, 0x6b)
      val t = if f <= 0.5 then f * 2.0 else (f - 0.5) * 2.0
      val r = (r1 + (r2 - r1) * t).toInt
      val g = (g1 + (g2 - g1) * t).toInt
      val b = (b1 + (b2 - b1) * t).toInt
      f"#$r%02x$g%02x$b%02x"

    def buildInsightCard(badge: String, title: String, detail: String, color: String): HBox =
      val badgeLbl = new Label(badge)
      badgeLbl.style = s"-fx-text-fill: $color; -fx-font-weight: bold; -fx-font-size: 14px; " +
        s"-fx-font-family: 'Consolas', monospace; -fx-min-width: 32;"
      val titleLbl = new Label(title)
      titleLbl.style = s"-fx-text-fill: ${Theme.TextBright}; -fx-font-size: 13px; " +
        s"-fx-font-weight: bold; -fx-font-family: 'Consolas', monospace;"
      val detailLbl = new Label(detail)
      detailLbl.style    = Theme.labelStyle(11, Theme.TextNormal)
      detailLbl.wrapText = true
      val textBox = new VBox(2)
      textBox.children.addAll(titleLbl.delegate, detailLbl.delegate)
      val card = new HBox(12)
      card.style     = Theme.cardStyle + s" -fx-padding: 12 14 12 14;"
      card.alignment = Pos.CenterLeft
      card.maxWidth  = Double.MaxValue
      card.children.addAll(badgeLbl.delegate, textBox.delegate)
      card

    def emptyState(icon: String, title: String, sub: String): StackPane =
      val iconLbl = new Label(icon)
      iconLbl.style = s"-fx-font-size: 40px;"
      val titleLbl = new Label(title)
      titleLbl.style = s"-fx-text-fill: ${Theme.TextDim}; -fx-font-size: 18px; " +
        s"-fx-font-weight: bold; -fx-font-family: 'Consolas', monospace;"
      val subLbl = new Label(sub)
      subLbl.style    = Theme.labelStyle(11, Theme.TextDim)
      subLbl.wrapText = true
      subLbl.maxWidth = 380
      val box = new VBox(10)
      box.alignment = Pos.Center
      box.children.addAll(iconLbl.delegate, titleLbl.delegate, subLbl.delegate)
      val sp = new StackPane
      sp.style = s"-fx-background-color: ${Theme.BgDeep};"
      sp.children.add(box.delegate)
      sp

    // ── SCALING VIEW ──────────────────────────────────────────
    def scalingView(): javafx.scene.Node =
      val warm = results.filter(_.isWarm).toSeq
      if warm.isEmpty then
        return emptyState("📈", "SCALING CURVE",
          "Run a benchmark across multiple array sizes to see how each algorithm scales.").delegate

      val agg     = aggregate(warm)
      val algos   = agg.map(_.algoName).distinct.sorted
      val sizes   = agg.map(_.size).distinct.sorted

      val xAxis = new NumberAxis
      xAxis.setLabel("Array Size (N)")
      xAxis.setAutoRanging(false)
      xAxis.setLowerBound(0)
      xAxis.setUpperBound(sizes.max * 1.1)
      xAxis.setTickUnit(sizes.max / 5.0)

      val yAxis = new NumberAxis
      yAxis.setLabel("Time (ms)")
      yAxis.setAutoRanging(true)

      val chart = new LineChart[Number, Number](xAxis, yAxis)
      chart.setTitle("")
      chart.setAnimated(false)
      chart.setCreateSymbols(true)
      chart.getStylesheets.add(darkChartStylesheet)
      applyChartCss(chart, algos.zipWithIndex.map { (_, i) => seriesColor(i) })

      algos.foreach { algo =>
        val series = new XYChart.Series[Number, Number]
        series.setName(algo)
        val points = agg
          .filter(_.algoName == algo)
          .groupBy(_.size)
          .map { (sz, rs) => sz -> rs.map(_.avgTimeMs).sum / rs.size }
          .toSeq.sortBy(_._1)
        points.foreach { (sz, ms) =>
          series.getData.add(new XYChart.Data[Number, Number](sz, ms))
        }
        chart.getData.add(series)
      }

      val wrapper = new VBox(0)
      wrapper.style = s"-fx-background-color: ${Theme.BgDeep};"
      VBox.setVgrow(chart, Priority.Always)
      HBox.setHgrow(wrapper, Priority.Always)
      javafx.scene.layout.VBox.setVgrow(chart, javafx.scene.layout.Priority.ALWAYS)
      wrapper.getChildren.add(chart)
      wrapper

    // ── COMPARISONS VIEW ──────────────────────────────────────
    def comparisonsView(): javafx.scene.Node =
      val warm = results.filter(_.isWarm).toSeq
      if warm.isEmpty then
        return emptyState("📊", "COMPARISONS",
          "Run a benchmark to see comparison counts per algorithm and data pattern.").delegate

      val agg      = aggregate(warm)
      val algos    = agg.map(_.algoName).distinct.sorted
      val patterns = agg.map(_.pattern).distinct.sorted
      val sizes    = agg.map(_.size).distinct.sorted

      def makeChart(sz: Int): BarChart[String, Number] =
        val sizeAgg = agg.filter(_.size == sz)
        val xAxis = new CategoryAxis
        xAxis.setLabel("Data Pattern")
        val yAxis = new NumberAxis
        yAxis.setLabel("Comparisons (avg)")
        yAxis.setAutoRanging(true)
        val chart = new BarChart[String, Number](xAxis, yAxis)
        chart.setTitle("")
        chart.setAnimated(false)
        chart.setCategoryGap(20)
        chart.setBarGap(3)
        chart.setPrefHeight(280)
        chart.getStylesheets.add(darkChartStylesheet)
        applyChartCss(chart, algos.zipWithIndex.map { (_, i) => seriesColor(i) })
        algos.foreach { algo =>
          val series = new XYChart.Series[String, Number]
          series.setName(algo)
          patterns.foreach { pat =>
            val pts = sizeAgg.filter(r => r.algoName == algo && r.pattern == pat)
            if pts.nonEmpty then
              val avgComps = pts.map(_.avgComparisons).sum / pts.size
              series.getData.add(new XYChart.Data[String, Number](pat, avgComps))
          }
          chart.getData.add(series)
        }
        chart

      val outerBox = new VBox(24)
      outerBox.style   = s"-fx-background-color: ${Theme.BgDeep};"
      outerBox.padding = Insets(20)

      sizes.foreach { sz =>
        val sizeLbl = new Label(s"N = $sz")
        sizeLbl.style = Theme.titleStyle(11)
        outerBox.children.addAll(sizeLbl.delegate, makeChart(sz))
      }

      val scroll = new ScrollPane
      scroll.setContent(outerBox)
      scroll.setFitToWidth(true)
      scroll.setStyle(Theme.scrollPaneStyle)
      scroll

    // ── HEATMAP VIEW ──────────────────────────────────────────
    def heatmapView(): javafx.scene.Node =
      val warm = results.filter(_.isWarm).toSeq
      if warm.isEmpty then
        return emptyState("🟩", "HEATMAP",
          "Run a benchmark to see algorithm × pattern performance ranked visually.").delegate

      val agg      = aggregate(warm)
      val algos    = agg.map(_.algoName).distinct.sorted
      val patterns = agg.map(_.pattern).distinct.sorted
      val sizes    = agg.map(_.size).distinct.sorted

      def makeGrid(sz: Int): GridPane =
        val sizeAgg = agg.filter(_.size == sz)
        val grid = new GridPane
        grid.setHgap(3)
        grid.setVgap(3)
        grid.setStyle(s"-fx-background-color: ${Theme.BgDeep};")

        val cornerLbl = new Label("ALGO \\ PATTERN")
        cornerLbl.setStyle(Theme.labelStyle(9, Theme.TextDim))
        grid.add(cornerLbl, 0, 0)

        patterns.zipWithIndex.foreach { (pat, col) =>
          val lbl = new Label(pat.take(14))
          lbl.setStyle(Theme.labelStyle(9, Theme.AccentPrimary))
          lbl.setRotate(-30)
          lbl.setMinWidth(80)
          grid.add(lbl, col + 1, 0)
        }

        val patternMinMax: Map[String, (Double, Double)] =
          patterns.map { pat =>
            val times = sizeAgg.filter(_.pattern == pat).map(_.avgTimeMs)
            val mn = if times.isEmpty then 0.0 else times.min
            val mx = if times.isEmpty then 1.0 else times.max
            pat -> (mn, mx.max(mn + 0.001))
          }.toMap

        algos.zipWithIndex.foreach { (algo, row) =>
          val algoLbl = new Label(algo)
          algoLbl.setStyle(Theme.labelStyle(11, Theme.TextNormal))
          algoLbl.setMinWidth(120)
          grid.add(algoLbl, 0, row + 1)

          patterns.zipWithIndex.foreach { (pat, col) =>
            val pts = sizeAgg.filter(r => r.algoName == algo && r.pattern == pat)
            val cell = new javafx.scene.layout.VBox(2)
            cell.setPrefWidth(90)
            cell.setPrefHeight(46)
            cell.setAlignment(javafx.geometry.Pos.CENTER)

            if pts.isEmpty then
              cell.setStyle(s"-fx-background-color: ${Theme.BgBorder}; -fx-background-radius: 3;")
            else
              val avgMs = pts.map(_.avgTimeMs).sum / pts.size
              val (mn, mx) = patternMinMax(pat)
              val fraction = (avgMs - mn) / (mx - mn)
              val hexColor = interpolateColor(fraction)

              cell.setStyle(
                s"-fx-background-color: ${hexColor}33; " +
                  s"-fx-border-color: ${hexColor}66; " +
                  s"-fx-border-radius: 3; -fx-background-radius: 3;"
              )

              val timeLbl = new javafx.scene.control.Label(f"$avgMs%.1f ms")
              timeLbl.setStyle(
                s"-fx-text-fill: $hexColor; -fx-font-size: 11px; " +
                  s"-fx-font-weight: bold; -fx-font-family: 'Consolas', monospace;"
              )
              val rankLbl = new javafx.scene.control.Label(
                if fraction <= 0.15 then "FAST" else if fraction >= 0.85 then "SLOW" else ""
              )
              rankLbl.setStyle(s"-fx-text-fill: $hexColor; -fx-font-size: 8px; " +
                s"-fx-font-family: 'Consolas', monospace;")
              cell.getChildren.addAll(timeLbl, rankLbl)

            grid.add(cell, col + 1, row + 1)
          }
        }
        grid

      val outerBox = new VBox(24)
      outerBox.style   = s"-fx-background-color: ${Theme.BgDeep};"
      outerBox.padding = Insets(20)

      sizes.foreach { sz =>
        val sizeLbl = new Label(s"N = $sz")
        sizeLbl.setStyle(Theme.titleStyle(11))
        outerBox.children.addAll(sizeLbl.delegate, makeGrid(sz))
      }

      val scroll = new ScrollPane
      scroll.setContent(outerBox)
      scroll.setFitToWidth(true)
      scroll.setStyle(Theme.scrollPaneStyle)
      scroll

    // ── INSIGHTS VIEW ─────────────────────────────────────────
    def insightsView(): javafx.scene.Node =
      val warm = results.filter(_.isWarm).toSeq
      if warm.isEmpty then
        return emptyState("💡", "INSIGHTS",
          "Run a benchmark to generate automatic performance insights.").delegate

      val agg      = aggregate(warm)
      val algos    = agg.map(_.algoName).distinct.sorted
      val patterns = agg.map(_.pattern).distinct.sorted
      val maxSize  = agg.map(_.size).maxOption.getOrElse(1)

      val cards = collection.mutable.ListBuffer.empty[HBox]

      // ── Insight 1: Fastest overall at largest N ───────────
      val largeN = agg.filter(_.size == maxSize)
      if largeN.nonEmpty then
        val best = largeN.minBy(_.avgTimeMs)
        val worst = largeN.maxBy(_.avgTimeMs)
        val ratio = worst.avgTimeMs / best.avgTimeMs.max(0.001)
        cards += buildInsightCard(
          "⚡", s"Fastest at N=$maxSize: ${best.algoName}",
          f"Averaged ${best.avgTimeMs}%.2f ms on ${best.pattern}. " +
            f"${worst.algoName} was $ratio%.1fx slower (${worst.avgTimeMs}%.2f ms). " +
            s"At large N, O(n log n) algorithms dominate O(n²) ones significantly.",
          Theme.AccentSuccess
        )

      // ── Insight 2: Best algorithm per pattern ─────────────
      patterns.foreach { pat =>
        val patResults = agg.filter(r => r.pattern == pat && r.size == maxSize)
        if patResults.nonEmpty then
          val winner = patResults.minBy(_.avgTimeMs)
          val second = patResults.sortBy(_.avgTimeMs).drop(1).headOption
          val detail = second.map { s =>
            f"${winner.avgTimeMs}%.2f ms vs ${s.algoName} at ${s.avgTimeMs}%.2f ms"
          }.getOrElse(f"${winner.avgTimeMs}%.2f ms")
          cards += buildInsightCard(
            "🏆", s"Best on $pat: ${winner.algoName}",
            detail,
            Theme.AccentPrimary
          )
      }

      // ── Insight 3: Worst case sensitivity ─────────────────
      algos.foreach { algo =>
        val algoResults = agg.filter(r => r.algoName == algo && r.size == maxSize)
        if algoResults.size >= 2 then
          val best  = algoResults.minBy(_.avgTimeMs)
          val worst = algoResults.maxBy(_.avgTimeMs)
          val spike = worst.avgTimeMs / best.avgTimeMs.max(0.001)
          if spike > 3.0 then
            cards += buildInsightCard(
              "⚠️", s"$algo is pattern-sensitive",
              f"Best case: ${best.pattern} (${best.avgTimeMs}%.2f ms). " +
                f"Worst case: ${worst.pattern} (${worst.avgTimeMs}%.2f ms). " +
                f"$spike%.1fx difference — this algorithm behaves very differently depending on input shape.",
              Theme.AccentSecondary
            )
      }

      // ── Insight 4: Memory efficiency ──────────────────────
      val heapResults = agg.filter(_.size == maxSize).groupBy(_.algoName)
        .map { (algo, rs) => algo -> rs.map(_.avgHeapDeltaMb).sum / rs.size }
        .toSeq.sortBy(_._2)

      if heapResults.nonEmpty then
        val (lowestAlgo, lowestHeap) = heapResults.head
        val (highestAlgo, highestHeap) = heapResults.last
        cards += buildInsightCard(
          "🧠", s"Most memory efficient: $lowestAlgo",
          f"Avg heap delta: $lowestHeap%.2f MB at N=$maxSize. " +
            f"$highestAlgo used the most heap ($highestHeap%.2f MB). " +
            s"In-place algorithms (Heap, Shell, Quick) typically show lower allocation.",
          Theme.AccentPrimary
        )

      // ── Insight 5: Comparison count leaders ───────────────
      val compResults = agg.filter(_.size == maxSize)
      if compResults.nonEmpty then
        val fewest = compResults.minBy(_.avgComparisons)
        val most   = compResults.maxBy(_.avgComparisons)
        cards += buildInsightCard(
          "🔍", s"Fewest comparisons: ${fewest.algoName}",
          f"Avg ${fewest.avgComparisons}%.0f comparisons at N=$maxSize. " +
            f"${most.algoName} made the most comparisons (${most.avgComparisons}%.0f). " +
            s"Fewer comparisons doesn't always mean faster — cache behaviour matters too.",
          Theme.AccentSuccess
        )

      // ── Insight 6: Stable sort recommendation ─────────────
      val stableResults = warm.filter(_.isStable).map(_.algoName).distinct
      val stableFast = agg
        .filter(r => stableResults.contains(r.algoName) && r.size == maxSize)
        .sortBy(_.avgTimeMs)
        .headOption
      stableFast.foreach { sf =>
        cards += buildInsightCard(
          "🔒", s"Fastest stable sort: ${sf.algoName}",
          f"${sf.algoName} is stable and averaged ${sf.avgTimeMs}%.2f ms at N=$maxSize. " +
            s"Stable sorts preserve the relative order of equal elements — critical for multi-key sorting.",
          Theme.AccentSecondary
        )
      }

      // ── Insight 7: GC pressure ────────────────────────────
      val gcResults = agg.filter(_.size == maxSize).sortBy(-_.avgGcCollections)
      if gcResults.nonEmpty && gcResults.head.avgGcCollections > 0 then
        val heaviest = gcResults.head
        cards += buildInsightCard(
          "♻️", s"Highest GC pressure: ${heaviest.algoName}",
          f"Triggered ${heaviest.avgGcCollections}%.0f GC collections avg at N=$maxSize. " +
            s"Algorithms that create temporary arrays (Merge Sort) put more pressure on the GC " +
            s"and can introduce unpredictable pauses in production workloads.",
          Theme.AccentDanger
        )

      // ── Layout ────────────────────────────────────────────
      val titleLbl = new Label("AUTO-GENERATED INSIGHTS")
      titleLbl.style = Theme.titleStyle(10)

      val subLbl = new Label(s"Based on ${warm.size} warm runs across ${algos.size} algorithms and ${patterns.size} patterns at sizes up to N=$maxSize")
      subLbl.style    = Theme.labelStyle(10, Theme.TextDim)
      subLbl.wrapText = true

      val cardsBox = new VBox(10)
      cardsBox.padding = Insets(0, 0, 20, 0)
      cards.foreach(c => cardsBox.children.add(c.delegate))

      val innerBox = new VBox(10)
      innerBox.padding = Insets(24, 28, 24, 28)
      innerBox.style   = s"-fx-background-color: ${Theme.BgDeep};"
      innerBox.children.addAll(titleLbl.delegate, subLbl.delegate, spacer(), cardsBox.delegate)

      val scroll = new ScrollPane
      scroll.setContent(innerBox)
      scroll.setFitToWidth(true)
      scroll.setStyle(Theme.scrollPaneStyle)
      scroll

    // ── Show logic ────────────────────────────────────────────
    def showTab(tab: AnalysisTab): Unit =
      contentArea.children.clear()
      val node = tab match
        case AnalysisTab.Scaling     => scalingView()
        case AnalysisTab.Comparisons => comparisonsView()
        case AnalysisTab.Heatmap     => heatmapView()
        case AnalysisTab.Insights    => insightsView()
      contentArea.children.add(node)

    val scalingItem    = miniNavItem("📈  SCALING",     AnalysisTab.Scaling,     () => showTab(AnalysisTab.Scaling))
    val compsItem      = miniNavItem("📊  COMPARISONS", AnalysisTab.Comparisons, () => showTab(AnalysisTab.Comparisons))
    val heatmapItem    = miniNavItem("🟩  HEATMAP",     AnalysisTab.Heatmap,     () => showTab(AnalysisTab.Heatmap))
    val insightsItem   = miniNavItem("💡  INSIGHTS",    AnalysisTab.Insights,    () => showTab(AnalysisTab.Insights))

    miniNav.children.addAll(
      scalingItem.delegate,
      compsItem.delegate,
      heatmapItem.delegate,
      insightsItem.delegate
    )

    // Rebuild current tab when new results arrive
    results.onChange { (_, _) =>
      Platform.runLater { showTab(currentTab) }
    }

    showTab(AnalysisTab.Scaling)

    val root = new BorderPane
    root.style  = s"-fx-background-color: ${Theme.BgDeep};"
    root.top    = miniNav
    root.center = contentArea
    root