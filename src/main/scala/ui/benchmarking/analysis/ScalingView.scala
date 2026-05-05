package ui.benchmarking.analysis

import javafx.scene.chart as jfxc
import scalafx.Includes.*
import scalafx.scene.chart.*
import scalafx.scene.layout.*
import ui.benchmarking.BenchmarkAnalysisPage.{AvgResult, aggregate, applyChartCss, seriesColor}
import ui.utils.Theme

object ScalingView:
  def build(filteredResults: Seq[AnyRef], darkChartStylesheet: String): javafx.scene.Node =
    if filteredResults.isEmpty then
      return ui.benchmarking.BenchmarkAnalysisPage.emptyState("📈", "SCALING CURVE", "Run benchmarks to see performance scaling.").delegate

    val agg = aggregate(filteredResults)
    val algos = agg.map(_.algoName).distinct.sorted

    // 1. Używamy składni ScalaFX do inicjalizacji (bez dziedziczenia)
    val xAxis = NumberAxis("Array Size (N)")
    xAxis.autoRanging = true

    val yAxis = NumberAxis("Time (ms)")
    yAxis.autoRanging = true

    // 2. Tworzymy LineChart przy użyciu ScalaFX wrapper
    val chart = new LineChart[Number, Number](xAxis, yAxis) {
      // W ScalaFX używamy 'animated' (to jest property), ale upewnij się, że importy są OK
      animated = false
      createSymbols = true
      stylesheets.add(darkChartStylesheet)
    }

    // 3. Konwersja kolorów CSS
    applyChartCss(chart, algos.zipWithIndex.map { (_, i) => seriesColor(i) })

    // 4. Budowanie serii danych
    algos.foreach { algo =>
      val series = jfxc.XYChart.Series[Number, Number]()
      series.setName(algo)

      agg.filter(_.algoName == algo).sortBy(_.size).foreach { pt =>
        series.getData.add(jfxc.XYChart.Data[Number, Number](pt.size, pt.avgTimeMs))
      }
      chart.getData.add(series)
    }

    // 5. Zwracamy delegata (czysty obiekt JavaFX)
    new VBox {
      style = s"-fx-background-color: ${Theme.BgDeep};"
      spacing = 10
      children = Seq(chart)
    }.delegate