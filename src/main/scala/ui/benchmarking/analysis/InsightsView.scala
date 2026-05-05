package ui.benchmarking.analysis

import scalafx.Includes.*
import scalafx.geometry.Insets
import scalafx.scene.control.{Label, ScrollPane}
import scalafx.scene.layout.*
import ui.benchmarking.BenchmarkAnalysisPage.{aggregate, buildInsightCard}
import ui.utils.Theme

object InsightsView:
  def build(filteredResults: Seq[AnyRef]): javafx.scene.Node =
    if filteredResults.isEmpty then
      return ui.benchmarking.BenchmarkAnalysisPage.emptyState("💡", "INSIGHTS", "No results to analyze.").delegate

    val agg = aggregate(filteredResults)
    val cardsBox = new VBox(10)
    val maxSize = agg.map(_.size).maxOption.getOrElse(0)

    // Performance Leader
    agg.filter(_.size == maxSize).sortBy(_.avgTimeMs).headOption.foreach { best =>
      cardsBox.children.add(buildInsightCard("⚡", "Performance Leader", s"${best.algoName} is fastest at N=$maxSize.", Theme.AccentSuccess))
    }

    // Memory Usage
    val memHeavy = agg.filter(_.size == maxSize).maxBy(_.avgHeapDeltaMb)
    if (memHeavy.avgHeapDeltaMb > 0.1) {
      cardsBox.children.add(buildInsightCard("🧠", "High Allocation", s"${memHeavy.algoName} used ${f"${memHeavy.avgHeapDeltaMb}%.2f"} MB heap.", Theme.AccentDanger))
    }

    // Comparison Leader
    agg.filter(_.size == maxSize).sortBy(_.avgComparisons).headOption.foreach { best =>
      cardsBox.children.add(buildInsightCard("🔍", "Most Efficient Logic", s"${best.algoName} performed the fewest comparisons.", Theme.AccentPrimary))
    }

    new ScrollPane {
      content = new VBox(15) {
        padding = Insets(20)
        children.addAll(new Label("ALGORITHM INSIGHTS") { style = Theme.titleStyle(12) }, cardsBox)
      }
      fitToWidth = true
      style = Theme.scrollPaneStyle
    }.delegate