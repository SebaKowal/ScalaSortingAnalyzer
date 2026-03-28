package ui

import app.AppState
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.Label
import scalafx.scene.layout.*
import scalafx.scene.shape.Rectangle
import scalafx.scene.paint.Color

class MetricsPanel(state: AppState):

  private def makeValue(color: String = Theme.TextBright): Label =
    val l = new Label("0")
    l.style = s"-fx-text-fill: $color; -fx-font-size: 20px; -fx-font-weight: bold; " +
      s"-fx-font-family: 'Consolas', monospace;"
    l

  private def metricCard(key: String, valueLbl: Label): VBox =
    val keyLbl = new Label(key)
    keyLbl.style = Theme.labelStyle(9, Theme.TextDim)
    val box = new VBox(2)
    box.padding  = Insets(8, 10, 8, 10)
    box.style    = Theme.cardStyle
    box.maxWidth = Double.MaxValue
    box.children.addAll(keyLbl.delegate, valueLbl.delegate)
    box

  private def legendRow(color: String, label: String): HBox =
    val dot = new javafx.scene.shape.Rectangle(8, 8)
    dot.setFill(javafx.scene.paint.Color.web(color))
    val lbl = new Label(label)
    lbl.style = Theme.labelStyle(10, Theme.TextNormal)
    val row = new HBox(6)
    row.alignment = Pos.CenterLeft
    row.children.addAll(dot, lbl.delegate)
    row

  private def spacer(h: Int = 6): Region =
    val r = new Region
    r.prefHeight = h
    r

  private def vGrow(): Region =
    val r = new Region
    VBox.setVgrow(r, Priority.Always)
    r

  private val compLbl = makeValue(Theme.AccentPrimary)
  private val swapLbl = makeValue(Theme.AccentSecondary)
  private val timeLbl = makeValue(Theme.TextBright)

  private val algoLbl = new Label("BUBBLE SORT")
  algoLbl.style    = s"-fx-text-fill: ${Theme.AccentPrimary}; -fx-font-size: 11px; " +
    s"-fx-font-weight: bold; -fx-font-family: 'Consolas', monospace;"
  algoLbl.wrapText = true

  private val descLbl = new Label("")
  descLbl.style    = Theme.labelStyle(10, Theme.TextNormal)
  descLbl.wrapText = true
  descLbl.maxWidth = Double.MaxValue

  state.comparisons.onChange { (_, _, v) => compLbl.text = f"${v.longValue()}%,d" }
  state.swaps.onChange       { (_, _, v) => swapLbl.text = f"${v.longValue()}%,d" }
  state.elapsedMs.onChange   { (_, _, v) => timeLbl.text = s"${v}ms" }

  state.selectedAlgorithm.onChange { (_, _, algo) =>
    algoLbl.text = algo.label.toUpperCase
    model.AlgorithmInfo.all.get(algo).foreach(i => descLbl.text = i.description)
  }

  model.AlgorithmInfo.all.get(state.selectedAlgorithm.value).foreach { info =>
    descLbl.text = info.description
    algoLbl.text = state.selectedAlgorithm.value.label.toUpperCase
  }

  private val metricsHdr = new Label("METRICS")
  metricsHdr.style = Theme.titleStyle(9)

  private val algoHdr = new Label("ALGORITHM")
  algoHdr.style = Theme.titleStyle(9)

  private val legendHdr = new Label("LEGEND")
  legendHdr.style = Theme.titleStyle(9)

  val panel: VBox = new VBox(0)
  panel.prefWidth = 200
  panel.minWidth  = 200
  panel.maxWidth  = 200
  panel.style     = s"${Theme.panelStyle()} -fx-border-color: ${Theme.BgBorder}; -fx-border-width: 0 0 0 1;"
  panel.padding   = Insets(14, 12, 14, 12)
  panel.children.addAll(
    metricsHdr.delegate,
    spacer(8),
    metricCard("COMPARISONS",   compLbl).delegate,
    spacer(6),
    metricCard("SWAPS / WRITES", swapLbl).delegate,
    spacer(6),
    metricCard("ELAPSED",       timeLbl).delegate,
    spacer(14),
    algoHdr.delegate,
    spacer(4),
    algoLbl.delegate,
    spacer(6),
    descLbl.delegate,
    vGrow(),
    legendHdr.delegate,
    spacer(6),
    legendRow(Theme.AccentPrimary,   "comparing").delegate,
    spacer(3),
    legendRow(Theme.AccentSecondary, "swapping").delegate,
    spacer(3),
    legendRow(Theme.AccentSuccess,   "sorted").delegate,
    spacer(3),
    legendRow(Theme.AccentMuted,     "unsorted").delegate
  )