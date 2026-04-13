package ui.pages

import benchmark.BenchmarkResult
import ui.Theme
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.Label
import scalafx.scene.layout.*
import scalafx.Includes.*
import javafx.scene.chart.{LineChart, BarChart, XYChart, NumberAxis, CategoryAxis}
import javafx.scene.control.{ComboBox, ScrollPane as JScrollPane}
import javafx.scene.layout.GridPane
import scalafx.application.Platform

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

    // ── Tab nav bar ───────────────────────────────────────────
    val miniNav = new HBox(0)
    miniNav.style     = s"-fx-background-color: ${Theme.BgDeep}; " +
      s"-fx-border-color: ${Theme.BgBorder}; -fx-border-width: 0 0 1 0;"
    miniNav.prefHeight = 38
    miniNav.alignment  = Pos.CenterLeft
    miniNav.padding    = Insets(0, 0, 0, 8)

    def miniNavItem(label: String, tab: AnalysisTab, showFn: () => Unit): HBox =
      val lbl = new Label(label)
      lbl.style   = Theme.labelStyle(11, Theme.TextDim)
      lbl.padding = Insets(0, 4, 0, 4)

      val indicator = new Region
      indicator.prefHeight = 2
      indicator.maxWidth   = Double.MaxValue
      indicator.style      = "-fx-background-color: transparent;"

      val item = new HBox(0)
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
        // reset all indicators then activate this one
        miniNav.children.foreach { node =>
          node.setStyle("-fx-cursor: hand;")
        }
        setActive(true)
        showFn()
      )
      item.delegate.setOnMouseEntered(_ =>
        if currentTab != tab then
          item.style = s"-fx-cursor: hand; -fx-background-color: ${Theme.BgRaised};"
      )
      item.delegate.setOnMouseExited(_ =>
        setActive(currentTab == tab)
      )
      item

    // ── Placeholder builder ───────────────────────────────────
    def placeholder(title: String, desc: String, icon: String): StackPane =
      val iconLbl = new Label(icon)
      iconLbl.style = s"-fx-font-size: 36px; -fx-text-fill: ${Theme.TextDim};"

      val titleLbl = new Label(title)
      titleLbl.style = s"-fx-text-fill: ${Theme.TextDim}; -fx-font-size: 18px; " +
        s"-fx-font-weight: bold; -fx-font-family: 'Consolas', monospace;"

      val descLbl = new Label(desc)
      descLbl.style    = Theme.labelStyle(11, Theme.TextDim)
      descLbl.wrapText = true
      descLbl.maxWidth = 400

      val noDataLbl = new Label("Run a benchmark first to see results here")
      noDataLbl.style = Theme.labelStyle(10, Theme.BgBorder)

      val box = new VBox(10)
      box.alignment = Pos.Center
      box.children.addAll(
        iconLbl.delegate, titleLbl.delegate,
        descLbl.delegate, spacer(8), noDataLbl.delegate
      )

      val sp = new StackPane
      sp.style = s"-fx-background-color: ${Theme.BgDeep};"
      sp.children.add(box.delegate)
      sp

    // ── Shared chart helpers ──────────────────────────────────
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
            avgHeapDeltaMb   = rs.map(_.heapDeltaMb).sum / rs.size,
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
        s".default-color$idx.chart-series-line { -fx-stroke: $color; }" +
        s".default-color$idx.chart-line-symbol { -fx-background-color: $color, white; }" +
        s".default-color$idx.chart-bar { -fx-bar-fill: $color; }"
      }.mkString
      val encoded = java.net.URLEncoder.encode(css, "UTF-8").replace("+", "%20")
      chart.getStylesheets.add(s"data:text/css,$encoded")

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
      card.style     = Theme.cardStyle + s" -fx-padding: 12;"
      card.alignment = Pos.CenterLeft
      card.children.addAll(badgeLbl.delegate, textBox.delegate)
      card

    // ── Tab content builders (placeholders for now) ───────────
    // Step 3 will replace scalingView with real LineChart
    def scalingView(): StackPane =
      if results.isEmpty then
        placeholder(
          "SCALING CURVE",
          "N vs Time line chart — one line per algorithm.\nShows O(n²) vs O(n log n) visually.",
          "📈"
        )
      else
        placeholder("SCALING CURVE", "Building chart…", "📈")

    // Step 4 will replace comparisonsView with real BarChart
    def comparisonsView(): StackPane =
      if results.isEmpty then
        placeholder(
          "COMPARISONS",
          "Grouped bar chart — comparisons per algorithm per data pattern.\nReveals best/worst case behavior.",
          "📊"
        )
      else
        placeholder("COMPARISONS", "Building chart…", "📊")

    // Step 5 will replace heatmapView with real GridPane heatmap
    def heatmapView(): StackPane =
      if results.isEmpty then
        placeholder(
          "HEATMAP",
          "Algorithm × Pattern grid colored by relative speed rank.\nGreen = fastest, red = slowest.",
          "🟩"
        )
      else
        placeholder("HEATMAP", "Building heatmap…", "🟩")

    // Step 6 will replace insightsView with real auto-generated text
    def insightsView(): StackPane =
      if results.isEmpty then
        placeholder(
          "INSIGHTS",
          "Auto-generated conclusions from your benchmark data.\nFinds winners, losers, JIT gains, pattern sensitivity.",
          "💡"
        )
      else
        placeholder("INSIGHTS", "Analysing results…", "💡")

    // ── Show functions ────────────────────────────────────────
    def showTab(tab: AnalysisTab): Unit =
      contentArea.children.clear()
      val node = tab match
        case AnalysisTab.Scaling     => scalingView()
        case AnalysisTab.Comparisons => comparisonsView()
        case AnalysisTab.Heatmap     => heatmapView()
        case AnalysisTab.Insights    => insightsView()
      contentArea.children.add(node.delegate)

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

    // Show initial tab
    showTab(AnalysisTab.Scaling)

    results.onChange { (_, _) =>
      Platform.runLater { showTab(currentTab) }
    }

    // ── Root ──────────────────────────────────────────────────
    val root = new BorderPane
    root.style  = s"-fx-background-color: ${Theme.BgDeep};"
    root.top    = miniNav
    root.center = contentArea
    root