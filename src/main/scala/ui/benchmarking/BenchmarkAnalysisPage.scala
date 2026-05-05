package ui.benchmarking

import benchmark.full.FullResult
import benchmark.pure.PureResult
import ui.benchmarking.analysis.*
import scalafx.Includes.*
import scalafx.application.Platform
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Label, MenuButton, MenuItem, ScrollPane}
import scalafx.scene.layout.*
import ui.utils.Theme

object BenchmarkAnalysisPage:

  // --- Shared Data Models ---
  case class AvgResult(
                        algoName: String, pattern: String, size: Int,
                        avgTimeMs: Double, avgComparisons: Double, avgSwaps: Double,
                        avgHeapDeltaMb: Double, avgGcCollections: Double
                      )

  // --- Logic Helpers ---
  private def toPure(r: AnyRef): PureResult = r match
    case p: PureResult => p
    case f: FullResult => f.pure
    case _             => throw new IllegalArgumentException(s"Unknown result type")

  def aggregate(data: Seq[AnyRef]): Seq[AvgResult] =
    data.groupBy(r => (toPure(r).algoName, toPure(r).pattern, toPure(r).size)).map {
      case ((algo, pat, sz), rs) =>
        val pures = rs.map(toPure)
        val fulls = rs.collect { case f: FullResult => f }
        AvgResult(algo, pat, sz,
          avgTimeMs        = pures.map(_.meanNs / 1e6).sum / rs.size,
          avgComparisons   = pures.map(_.comparisons.toDouble).sum / rs.size,
          avgSwaps         = pures.map(_.swaps.toDouble).sum / rs.size,
          avgHeapDeltaMb   = if fulls.isEmpty then 0.0 else fulls.map(_.heapDeltaMb).sum / fulls.size,
          avgGcCollections = if fulls.isEmpty then 0.0 else fulls.map(_.gcCollections.toDouble).sum / fulls.size
        )
    }.toSeq

  // --- UI Styling Helpers ---
  val palette = Seq("#00d4ff", "#ff8c00", "#00ff9d", "#ff2d6b", "#a855f7", "#facc15")
  def seriesColor(idx: Int): String = palette(idx % palette.size)

  def applyChartCss(chart: javafx.scene.chart.XYChart[?, ?], colors: Seq[String]): Unit =
    val css = colors.zipWithIndex.map { (c, i) =>
      s".default-color$i.chart-series-line { -fx-stroke: $c; } .default-color$i.chart-bar { -fx-bar-fill: $c; }"
    }.mkString("\n")
    chart.getStylesheets.add(s"data:text/css,${java.net.URLEncoder.encode(css, "UTF-8")}")

  def interpolateColor(f: Double): String =
    val ratio = f.max(0.0).min(1.0)
    if ratio <= 0.5 then "#00ff9d" else "#ff2d6b"

  def buildInsightCard(badge: String, title: String, detail: String, color: String): HBox =
    new HBox(12) {
      style = Theme.cardStyle + "; -fx-padding: 12;"
      alignment = Pos.CenterLeft
      children.addAll(
        new Label(badge) { style = s"-fx-text-fill: $color; -fx-font-size: 16px; -fx-font-weight: bold;" },
        new VBox(2) {
          children.addAll(
            new Label(title) { style = s"-fx-text-fill: ${Theme.TextBright}; -fx-font-weight: bold;" },
            new Label(detail) { style = Theme.labelStyle(11, Theme.TextNormal); wrapText = true }
          )
        }
      )
    }

  def emptyState(icon: String, title: String, sub: String): StackPane =
    new StackPane {
      style = s"-fx-background-color: ${Theme.BgDeep};"
      children.add(new VBox(10) {
        alignment = Pos.Center
        children.addAll(
          new Label(icon) { style = "-fx-font-size: 40px;" },
          new Label(title) { style = Theme.titleStyle(16) },
          new Label(sub) { style = Theme.labelStyle(12, Theme.TextDim) }
        )
      })
    }

  // --- Main View Builder ---
  def build(results: ObservableBuffer[AnyRef]): BorderPane =
    val contentArea = new StackPane()
    var filterMode = "All"

    enum AnalysisTab:
      case Scaling, Comparisons, Heatmap, Insights
    var currentTab = AnalysisTab.Scaling

    def refresh(): Unit =
      val filtered = results.filter { r =>
        filterMode == "All" ||
          (filterMode == "Pure" && r.isInstanceOf[PureResult]) ||
          (filterMode == "Full" && r.isInstanceOf[FullResult])
      }.toSeq

      val view = currentTab match
        case AnalysisTab.Scaling     => ScalingView.build(filtered, "/* chart css */")
        case AnalysisTab.Comparisons => ComparisonsView.build(filtered, "/* chart css */")
        case AnalysisTab.Heatmap     => HeatMapView.build(filtered)
        case AnalysisTab.Insights    => InsightsView.build(filtered)

      contentArea.children.setAll(view)

    // Navigation logic and layout
    val nav = new HBox(10) {
      padding = Insets(10)
      style = s"-fx-background-color: ${Theme.BgBase}; -fx-border-color: ${Theme.BgBorder}; -fx-border-width: 0 0 1 0;"
      children.addAll(
        new MenuButton(s"Filter: $filterMode") {
          items.addAll(
            new MenuItem("All")  { onAction = _ => { filterMode = "All";  text = "Filter: All";  refresh() } },
            new MenuItem("Pure") { onAction = _ => { filterMode = "Pure"; text = "Filter: Pure"; refresh() } },
            new MenuItem("Full") { onAction = _ => { filterMode = "Full"; text = "Filter: Full"; refresh() } }
          )
        }
      )
    }

    results.onChange { (_, _) => Platform.runLater(refresh()) }
    refresh()

    new BorderPane {
      top = nav
      center = contentArea
    }