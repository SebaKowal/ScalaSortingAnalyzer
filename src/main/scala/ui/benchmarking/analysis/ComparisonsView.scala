package ui.benchmarking.analysis

import javafx.scene.chart.*
import scalafx.Includes.*
import scalafx.geometry.Insets
import scalafx.scene.control.{Label, ScrollPane}
import scalafx.scene.layout.*
import scalafx.scene.chart.{BarChart, CategoryAxis, NumberAxis, XYChart}
import ui.benchmarking.BenchmarkAnalysisPage.{aggregate, applyChartCss, seriesColor}
import ui.utils.Theme

object ComparisonsView:
  def build(filteredResults: Seq[AnyRef], darkChartStylesheet: String): javafx.scene.Node =
    if filteredResults.isEmpty then
      return ui.benchmarking.BenchmarkAnalysisPage.emptyState("📊", "COMPARISONS", "No valid data to compare.").delegate

    val agg = aggregate(filteredResults)
    val algos = agg.map(_.algoName).distinct.sorted
    val patterns = agg.map(_.pattern).distinct.sorted
    val sizes = agg.map(_.size).distinct.sorted

    val outerBox = new VBox(24)
    outerBox.style = s"-fx-background-color: ${Theme.BgDeep};"
    outerBox.padding = Insets(20)

    sizes.foreach { sz =>
      // FIX: Use constructor and then set properties to avoid "Illegal inheritance"
      val xAxis = new CategoryAxis()
      xAxis.label = "Pattern"

      val yAxis = new NumberAxis()
      yAxis.label = "Comparisons"
      yAxis.autoRanging = true

      val chart = new BarChart[String, Number](xAxis, yAxis)
      chart.animated = false
      chart.prefHeight = 300
      chart.stylesheets.add(darkChartStylesheet)

      applyChartCss(chart, algos.zipWithIndex.map { (_, i) => seriesColor(i) })

      algos.foreach { algo =>
        // FIX: Instantiate and set name directly
        val series = new XYChart.Series[String, Number]()
        series.name = algo

        patterns.foreach { pat =>
          agg.find(r => r.size == sz && r.algoName == algo && r.pattern == pat).foreach { pt =>
            // Using the ScalaFX Data factory for cleaner syntax
            series.getData.add(XYChart.Data[String, Number](pat, pt.avgComparisons))
          }
        }
        chart.getData.add(series)
      }

      val sizeLabel = new Label(s"N = $sz")
      sizeLabel.style = Theme.titleStyle(11)
      outerBox.children.addAll(sizeLabel, chart)
    }

    val scroll = new ScrollPane()
    scroll.content = outerBox
    scroll.fitToWidth = true
    scroll.style = Theme.scrollPaneStyle
    scroll.delegate