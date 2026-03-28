package ui

import app.AppState
import model.AlgorithmInfo
import scalafx.geometry.Insets
import scalafx.scene.control.Label
import scalafx.scene.layout.VBox

class MetricsPanel(state: AppState):

  private def metricLabel(name: String): Label = new Label(name):
    style = "-fx-text-fill: #aaaacc; -fx-font-size: 11px;"

  private def valueLabel(): Label = new Label("0"):
    style = "-fx-text-fill: #e0e0ff; -fx-font-size: 15px; -fx-font-weight: bold;"

  private val compLabel  = valueLabel()
  private val swapLabel  = valueLabel()
  private val timeLabel  = valueLabel()
  private val statusLbl  = new Label("Ready"):
    style = "-fx-text-fill: #80cbc4; -fx-font-size: 12px;"
    wrapText = true

  private val algoInfoBox = new VBox(4):
    style = "-fx-background-color: #252840; -fx-background-radius: 8; -fx-padding: 10;"

  private val descLabel = new Label(""):
    wrapText = true
    maxWidth = 240
    style = "-fx-text-fill: #b0b8d8; -fx-font-size: 11px;"

  private val complexityLabel = new Label(""):
    wrapText = true
    maxWidth = 240
    style = "-fx-text-fill: #90caf9; -fx-font-size: 11px;"

  algoInfoBox.children = Seq(descLabel, complexityLabel)

  state.comparisons.onChange((_, _, v) => compLabel.text = v.toString)
  state.swaps.onChange((_, _, v)       => swapLabel.text = v.toString)
  state.elapsedMs.onChange((_, _, v)   => timeLabel.text = s"${v.toString} ms")
  state.statusMessage.onChange((_, _, v) => statusLbl.text = v.toString)

  state.selectedAlgorithm.onChange { (_, _, algo) =>
    AlgorithmInfo.all.get(algo).foreach { info =>
      descLabel.text = info.description
      complexityLabel.text =
        s"Best: ${algo.bestCase}  Avg: ${algo.avgCase}  Worst: ${algo.worstCase}  Space: ${algo.space}"
    }
  }
  // Init
  AlgorithmInfo.all.get(state.selectedAlgorithm.value).foreach { info =>
    descLabel.text = info.description
    val a = state.selectedAlgorithm.value
    complexityLabel.text = s"Best: ${a.bestCase}  Avg: ${a.avgCase}  Worst: ${a.worstCase}  Space: ${a.space}"
  }

  val panel: VBox = new VBox(12):
    padding = Insets(16)
    prefWidth = 260
    style = "-fx-background-color: #1a1d2e;"
    children = Seq(
      sectionTitle("Metrics"),
      metricRow("Comparisons", compLabel),
      metricRow("Swaps",       swapLabel),
      metricRow("Elapsed",     timeLabel),
      metricRow("Status",      statusLbl),
      sectionTitle("Complexity"),
      algoInfoBox
    )

  private def sectionTitle(t: String): Label = new Label(t):
    style = "-fx-text-fill: #7986cb; -fx-font-size: 13px; -fx-font-weight: bold;"

  private def metricRow(name: String, value: Label): VBox =
    new VBox(2, metricLabel(name), value)