package ui.panel

import app.AppState
import model.{AlgorithmDetail, AlgorithmInfo, AlgorithmType}
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.*
import scalafx.scene.layout.*
import ui.Theme
import scalafx.Includes.*

class RightPanel(state: AppState):

  private def hdr(text: String): Label =
    val l = new Label(text)
    l.style = Theme.titleStyle(9)
    l

  private def spacer(h: Int = 6): Region =
    val r = new Region; r.prefHeight = h; r

  private def vGrow(): Region =
    val r = new Region
    VBox.setVgrow(r, Priority.Always)
    r

  private def badge(key: String, value: String, color: String): VBox =
    val k = new Label(key)
    k.style = Theme.labelStyle(7, Theme.TextDim)
    val v = new Label(value)
    v.style    = s"-fx-text-fill: $color; -fx-font-size: 11px; " +
      s"-fx-font-weight: bold; -fx-font-family: 'Consolas', monospace;"
    v.wrapText = false
    // shrink text if it overflows
    v.minWidth = 10
    val box = new VBox(1)
    box.padding  = Insets(5, 7, 5, 7)
    box.style    = Theme.cardStyle
    box.prefWidth = 110
    box.maxWidth  = 110
    box.minWidth  = 10
    box.children.addAll(k.delegate, v.delegate)
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

  // ── Reactive labels ───────────────────────────────────────────
  private val algoNameLbl = new Label("")
  algoNameLbl.style    = s"-fx-text-fill: ${Theme.AccentPrimary}; -fx-font-size: 14px; " +
    s"-fx-font-weight: bold; -fx-font-family: 'Consolas', monospace;"
  algoNameLbl.wrapText = true

  private val topBadgeRow = new HBox(6)
  topBadgeRow.maxWidth = Double.MaxValue

  private val botBadgeRow = new HBox(6)
  botBadgeRow.maxWidth = Double.MaxValue

  private val complexityGrid = new VBox(5)
  complexityGrid.maxWidth = Double.MaxValue
  complexityGrid.children.addAll(topBadgeRow.delegate, botBadgeRow.delegate)

  private val descLbl = new Label("")
  descLbl.style    = Theme.labelStyle(11, Theme.TextNormal)
  descLbl.wrapText = true
  descLbl.maxWidth = Double.MaxValue

  private val timeLbl = new Label("")
  timeLbl.style    = Theme.labelStyle(11, Theme.TextNormal)
  timeLbl.wrapText = true
  timeLbl.maxWidth = Double.MaxValue

  private val spaceLbl = new Label("")
  spaceLbl.style    = Theme.labelStyle(11, Theme.TextNormal)
  spaceLbl.wrapText = true
  spaceLbl.maxWidth = Double.MaxValue

  private def refresh(algo: AlgorithmType): Unit =
    algoNameLbl.text = algo.label.toUpperCase

    val badges = Seq(
      ("BEST",  algo.bestCase,  Theme.AccentSuccess),
      ("AVG",   algo.avgCase,   Theme.AccentSecondary),
      ("WORST", algo.worstCase, Theme.AccentDanger),
      ("SPACE", algo.space,     Theme.AccentPrimary)
    )

    topBadgeRow.children.clear()
    botBadgeRow.children.clear()
    badges.zipWithIndex.foreach { case ((k, v, c), i) =>
      val b = badge(k, v, c)
      if i < 2 then topBadgeRow.children.add(b.delegate)
      else           botBadgeRow.children.add(b.delegate)
    }

    AlgorithmInfo.all.get(algo).foreach { info =>
      descLbl.text = info.description
    }

    AlgorithmDetail.all.get(algo).foreach { detail =>
      timeLbl.text  = detail.timeComplexityNotes
      spaceLbl.text = detail.spaceNotes
    }

  refresh(state.selectedAlgorithm.value)
  state.selectedAlgorithm.onChange { (_, _, algo) => refresh(algo) }

  private val cmpHdr    = hdr("COMPLEXITY")
  private val descHdr   = hdr("DESCRIPTION")
  private val timeHdr   = hdr("TIME NOTES")
  private val spaceHdr  = hdr("SPACE")
  private val legendHdr = hdr("LEGEND")

  VBox.setMargin(cmpHdr,    Insets(10, 0, 4, 0))
  VBox.setMargin(descHdr,   Insets(10, 0, 4, 0))
  VBox.setMargin(timeHdr,   Insets(10, 0, 4, 0))
  VBox.setMargin(spaceHdr,  Insets(10, 0, 4, 0))
  VBox.setMargin(legendHdr, Insets(10, 0, 6, 0))

  val panel: VBox = new VBox(0)
  panel.prefWidth = 260
  panel.minWidth  = 240
  panel.maxWidth  = 300
  panel.style     = s"${Theme.panelStyle()} -fx-border-color: ${Theme.BgBorder}; " +
    s"-fx-border-width: 0 0 0 1;"
  panel.padding   = Insets(14, 12, 14, 12)

  panel.children.addAll(
    algoNameLbl.delegate,
    spacer(2),
    cmpHdr.delegate,
    spacer(4),
    complexityGrid.delegate,
    descHdr.delegate,
    descLbl.delegate,
    timeHdr.delegate,
    timeLbl.delegate,
    spaceHdr.delegate,
    spaceLbl.delegate,
    vGrow(),
    legendHdr.delegate,
    spacer(4),
    legendRow(Theme.AccentPrimary,   "comparing").delegate,
    spacer(3),
    legendRow(Theme.AccentSecondary, "swapping").delegate,
    spacer(3),
    legendRow(Theme.AccentSuccess,   "sorted").delegate,
    spacer(3),
    legendRow(Theme.AccentMuted,     "unsorted").delegate
  )