package ui

import app.AppState
import model.{AlgorithmType, GeneratorType}
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.*
import scalafx.scene.layout.*

class ControlPanel(state: AppState, viz: VisualizerPanel):

  private def sectionLabel(text: String): Label =
    val l = new Label(text)
    l.style = Theme.titleStyle(9)
    l

  private def makeCombo(items: Seq[String], initial: String): ComboBox[String] =
    val cb = new ComboBox[String]
    cb.items = scalafx.collections.ObservableBuffer(items*)
    cb.value = initial
    cb.maxWidth = Double.MaxValue
    cb.style = Theme.comboStyle
    cb

  private def makeSlider(min: Double, max: Double, init: Double): Slider =
    val s = new Slider(min, max, init)
    s.showTickLabels = false
    s.showTickMarks  = false
    s.style = Theme.sliderStyle
    s.maxWidth = Double.MaxValue
    s

  private def makeButton(text: String, style: String): Button =
    val b = new Button(text)
    b.style = style
    b.maxWidth = Double.MaxValue
    b

  // ── Controls ─────────────────────────────────────────────────
  private val algoBox = makeCombo(
    AlgorithmType.values.map(_.label).toSeq,
    state.selectedAlgorithm.value.label
  )
  algoBox.onAction = _ =>
    AlgorithmType.values.find(_.label == algoBox.value.value).foreach { a =>
      state.selectedAlgorithm.value = a
    }

  private val genBox = makeCombo(
    GeneratorType.values.map(_.label).toSeq,
    state.selectedGenerator.value.label
  )
  genBox.onAction = _ =>
    GeneratorType.values.find(_.label == genBox.value.value).foreach { g =>
      state.selectedGenerator.value = g
    }

  private val sizeSlider     = makeSlider(10, 200, state.arraySize.value)
  private val sizeValueLbl   = new Label(state.arraySize.value.toString)
  sizeValueLbl.style         = Theme.labelStyle(11, Theme.AccentPrimary)
  sizeSlider.value.onChange { (_, _, v) =>
    state.arraySize.value = v.intValue()
    sizeValueLbl.text = v.intValue().toString
  }

  private val speedSlider    = makeSlider(1, 200, state.animationSpeed.value)
  private val speedValueLbl  = new Label(s"${state.animationSpeed.value} ms")
  speedValueLbl.style        = Theme.labelStyle(11, Theme.AccentPrimary)
  speedSlider.value.onChange { (_, _, v) =>
    state.animationSpeed.value = v.intValue()
    speedValueLbl.text = s"${v.intValue()} ms"
  }

  private val btnStart    = makeButton("▶  START",    Theme.buttonPrimary)
  private val btnPause    = makeButton("⏸  PAUSE",    Theme.buttonSecondary)
  private val btnReset    = makeButton("↺  RESET",    Theme.buttonSecondary)
  private val btnGenerate = makeButton("⟳  NEW ARRAY", Theme.buttonSecondary)
  btnPause.disable = true

  btnStart.onAction = _ =>
    if !state.isRunning.value then viz.startSort()

  btnPause.onAction = _ =>
    if state.isPaused.value then
      viz.resumeSort(); btnPause.text = "⏸  PAUSE"
    else
      viz.pauseSort(); btnPause.text = "▶  RESUME"

  btnReset.onAction = _ =>
    viz.resetArray(); btnPause.text = "⏸  PAUSE"

  btnGenerate.onAction = _ =>
    if !state.isRunning.value then viz.resetArray()

  state.isRunning.onChange { (_, _, running) =>
    btnStart.disable    = running
    btnGenerate.disable = running
    genBox.disable      = running
    sizeSlider.disable  = running
    btnPause.disable    = !running
    if !running then btnPause.text = "⏸  PAUSE"
  }

  // ── Complexity box ───────────────────────────────────────────
  private val complexityBox = new VBox(3)
  complexityBox.style   = Theme.cardStyle
  complexityBox.padding = Insets(8)
  complexityBox.maxWidth = Double.MaxValue

  private def updateComplexity(): Unit =
    complexityBox.children.clear()
    val a = state.selectedAlgorithm.value
    Seq(
      ("BEST",  a.bestCase,  Theme.AccentSuccess),
      ("AVG",   a.avgCase,   Theme.AccentSecondary),
      ("WORST", a.worstCase, Theme.AccentDanger),
      ("SPACE", a.space,     Theme.AccentPrimary)
    ).foreach { (k, v, c) =>
      val row    = new HBox(6)
      row.alignment = Pos.CenterLeft
      val keyLbl = new Label(k)
      keyLbl.style    = Theme.labelStyle(9, Theme.TextDim)
      keyLbl.prefWidth = 38
      val valLbl = new Label(v)
      valLbl.style = Theme.labelStyle(11, c)
      row.children.addAll(keyLbl.delegate, valLbl.delegate)
      complexityBox.children.add(row.delegate)
    }

  updateComplexity()
  state.selectedAlgorithm.onChange { (_, _, _) => updateComplexity() }

  // ── Status ───────────────────────────────────────────────────
  private val statusLbl = new Label("READY")
  statusLbl.style    = Theme.labelStyle(10, Theme.TextDim)
  statusLbl.maxWidth = Double.MaxValue
  statusLbl.wrapText = true

  state.statusMessage.onChange { (_, _, v) =>
    val s = v.toString
    statusLbl.text = s.toUpperCase
    val col =
      if s.startsWith("Done")    then Theme.AccentSuccess
      else if s.startsWith("Running") then Theme.AccentPrimary
      else if s == "Stopped"     then Theme.AccentDanger
      else Theme.TextDim
    statusLbl.style = Theme.labelStyle(10, col)
  }

  // ── Slider rows ──────────────────────────────────────────────
  private val sizeRow = new HBox(8)
  sizeRow.alignment = Pos.CenterLeft
  HBox.setHgrow(sizeSlider, Priority.Always)
  sizeRow.children.addAll(sizeSlider.delegate, sizeValueLbl.delegate)

  private val speedRow = new HBox(8)
  speedRow.alignment = Pos.CenterLeft
  HBox.setHgrow(speedSlider, Priority.Always)
  speedRow.children.addAll(speedSlider.delegate, speedValueLbl.delegate)

  private val btnRow = new HBox(4)
  HBox.setHgrow(btnReset,    Priority.Always)
  HBox.setHgrow(btnGenerate, Priority.Always)
  btnRow.children.addAll(btnReset.delegate, btnGenerate.delegate)

  // ── Spacers ──────────────────────────────────────────────────
  private def spacer(h: Int = 6): Region =
    val r = new Region
    r.prefHeight = h
    r

  private def vGrow(): Region =
    val r = new Region
    VBox.setVgrow(r, Priority.Always)
    r

  // ── Panel assembly ───────────────────────────────────────────
  val panel: VBox = new VBox(0)
  panel.prefWidth = 220
  panel.minWidth  = 220
  panel.maxWidth  = 220
  panel.style     = s"${Theme.panelStyle()} -fx-border-color: ${Theme.BgBorder}; -fx-border-width: 0 1 0 0;"
  panel.padding   = Insets(14, 12, 14, 12)

  private val algoLblHeader = sectionLabel("ALGORITHM")
  private val genLblHeader  = sectionLabel("DATA PATTERN")
  private val sizeLblHeader = sectionLabel("ARRAY SIZE")
  private val spdLblHeader  = sectionLabel("STEP DELAY")
  private val cmpLblHeader  = sectionLabel("COMPLEXITY")

  VBox.setMargin(genLblHeader,  Insets(10, 0, 3, 0))
  VBox.setMargin(sizeLblHeader, Insets(10, 0, 3, 0))
  VBox.setMargin(spdLblHeader,  Insets(8,  0, 3, 0))
  VBox.setMargin(cmpLblHeader,  Insets(10, 0, 4, 0))

  panel.children.addAll(
    algoLblHeader.delegate,
    algoBox.delegate,
    genLblHeader.delegate,
    genBox.delegate,
    sizeLblHeader.delegate,
    sizeRow.delegate,
    spdLblHeader.delegate,
    speedRow.delegate,
    cmpLblHeader.delegate,
    complexityBox.delegate,
    vGrow(),
    btnStart.delegate,
    spacer(4),
    btnPause.delegate,
    spacer(4),
    btnRow.delegate,
    spacer(8),
    statusLbl.delegate
  )