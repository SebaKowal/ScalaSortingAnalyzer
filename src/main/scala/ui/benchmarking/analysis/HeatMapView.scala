package ui.benchmarking.analysis

import javafx.scene.layout.GridPane
import scalafx.Includes.*
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.{Label, ScrollPane}
import scalafx.scene.layout.*
import ui.benchmarking.BenchmarkAnalysisPage.{aggregate, interpolateColor}
import ui.utils.Theme

object HeatMapView:
  def build(filteredResults: Seq[AnyRef]): javafx.scene.Node =
    if filteredResults.isEmpty then
      return ui.benchmarking.BenchmarkAnalysisPage.emptyState("🟩", "HEATMAP", "No data to show heatmap.").delegate

    val agg = aggregate(filteredResults)
    val algos = agg.map(_.algoName).distinct.sorted
    val patterns = agg.map(_.pattern).distinct.sorted
    val sizes = agg.map(_.size).distinct.sorted

    val outerBox = new VBox(24)
    outerBox.style = s"-fx-background-color: ${Theme.BgDeep};"
    outerBox.padding = Insets(20)

    sizes.foreach { sz =>
      // FIX: Explicitly set properties on the instance
      val grid = new GridPane()
      grid.setHgap(3)
      grid.setVgap(3)

      patterns.zipWithIndex.foreach { (pat, col) =>
        val patLabel = new Label(pat)
        patLabel.style = Theme.labelStyle(9, Theme.AccentPrimary)
        patLabel.rotate = -30
        patLabel.minWidth = 60
        grid.add(patLabel.delegate, col + 1, 0)
      }

      val patLimits = patterns.map { p =>
        val times = agg.filter(r => r.size == sz && r.pattern == p).map(_.avgTimeMs)
        p -> (if (times.isEmpty) 0.0 else times.min, if (times.isEmpty) 1.0 else (times.max max (times.min + 0.01)))
      }.toMap

      algos.zipWithIndex.foreach { (algo, row) =>
        val algoLabel = new Label(algo)
        algoLabel.style = Theme.labelStyle(10, Theme.TextNormal)
        algoLabel.minWidth = 100
        grid.add(algoLabel.delegate, 0, row + 1)

        patterns.zipWithIndex.foreach { (pat, col) =>
          agg.find(r => r.size == sz && r.algoName == algo && r.pattern == pat).foreach { pt =>
            val (min, max) = patLimits(pat)
            val frac = (pt.avgTimeMs - min) / (max - min)
            val color = interpolateColor(frac)

            val cell = new VBox()
            cell.alignment = Pos.Center
            cell.prefWidth = 80
            cell.prefHeight = 40
            cell.style = s"-fx-background-color: ${color}33; -fx-border-color: ${color}66; -fx-background-radius: 3;"

            val valLabel = new Label(f"${pt.avgTimeMs}%.1f ms")
            valLabel.style = s"-fx-text-fill: $color; -fx-font-size: 10px; -fx-font-weight: bold;"

            cell.children.add(valLabel)
            grid.add(cell.delegate, col + 1, row + 1)
          }
        }
      }

      val sizeHeader = new Label(s"N = $sz")
      sizeHeader.style = Theme.titleStyle(11)
      outerBox.children.addAll(sizeHeader, grid)
    }

    val scroll = new ScrollPane()
    scroll.content = outerBox
    scroll.fitToWidth = true
    scroll.style = Theme.scrollPaneStyle
    scroll.delegate