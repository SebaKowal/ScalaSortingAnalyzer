package ui.panel

import app.AppState
import benchmark.SystemMonitor
import model.{AlgorithmType, GeneratorType}
import scalafx.animation.{Animation, KeyFrame, Timeline}
import scalafx.util.Duration
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.*
import scalafx.scene.layout.*
import ui.{Theme, VisualizerPanel}

class LeftPanel(state: AppState, viz: VisualizerPanel):

  // ── Helpers ──────────────────────────────────────────────────
  private def hdr(text: String): Label =
    val l = new Label(text)
    l.style = Theme.titleStyle(9)
    l

  private def makeCombo(items: Seq[String], initial: String): ComboBox[String] =
    val cb = new ComboBox[String]
    cb.items = scalafx.collections.ObservableBuffer(items *)
    cb.value = initial
    cb.maxWidth = Double.MaxValue
    cb.style = Theme.comboStyle
    cb.delegate.getStylesheets.add(Theme.comboBoxStylesheet)
    cb

  private def makeSlider(min: Double, max: Double, init: Double): Slider =
    val s = new Slider(min, max, init)
    s.showTickLabels = false; s.showTickMarks = false
    s.style = Theme.sliderStyle; s.maxWidth = Double.MaxValue
    s

  private def makeBtn(text: String, sty: String): Button =
    val b = new Button(text)
    b.style = sty; b.maxWidth = Double.MaxValue
    b

  private def spacer(h: Int = 6): Region =
    val r = new Region; r.prefHeight = h; r

  private def vGrow(): Region =
    val r = new Region; VBox.setVgrow(r, Priority.Always); r

  private def divider(): Region =
    val r = new Region
    r.prefHeight = 1
    r.maxWidth = Double.MaxValue
    r.style = s"-fx-background-color: ${Theme.BgBorder};"
    VBox.setMargin(r, Insets(10, 0, 10, 0))
    r

  private def statRow(key: String, valLbl: Label): HBox =
    val k = new Label(key)
    k.style    = Theme.labelStyle(9, Theme.TextDim)
    k.prefWidth = 90
    val sp = new Region
    HBox.setHgrow(sp, Priority.Always)
    val row = new HBox(4)
    row.alignment = Pos.CenterLeft
    row.children.addAll(k.delegate, sp, valLbl.delegate)
    row

  private def makeStatVal(color: String = Theme.TextBright): Label =
    val l = new Label("—")
    l.style = s"-fx-text-fill: $color; -fx-font-size: 12px; " +
      s"-fx-font-weight: bold; -fx-font-family: 'Consolas', monospace;"
    l

  // ── Algorithm / generator selectors ─────────────────────────
  private val algoBox = makeCombo(
    AlgorithmType.values.map(_.label).toSeq,
    state.selectedAlgorithm.value.label
  )
  algoBox.onAction = _ =>
    AlgorithmType.values.find(_.label == algoBox.value.value)
      .foreach(state.selectedAlgorithm.value = _)

  private val genBox = makeCombo(
    GeneratorType.values.map(_.label).toSeq,
    state.selectedGenerator.value.label
  )
  genBox.onAction = _ =>
    GeneratorType.values.find(_.label == genBox.value.value)
      .foreach(state.selectedGenerator.value = _)

  // ── Sliders ──────────────────────────────────────────────────
  private val sizeSlider   = makeSlider(10, 200, state.arraySize.value)
  private val sizeValLbl   = new Label(state.arraySize.value.toString)
  sizeValLbl.style         = Theme.labelStyle(11, Theme.AccentPrimary)
  sizeSlider.value.onChange { (_, _, v) =>
    state.arraySize.value = v.intValue()
    sizeValLbl.text = v.intValue().toString
  }

  private val speedSlider  = makeSlider(1, 200, state.animationSpeed.value)
  private val speedValLbl  = new Label(s"${state.animationSpeed.value}ms")
  speedValLbl.style        = Theme.labelStyle(11, Theme.AccentPrimary)
  speedSlider.value.onChange { (_, _, v) =>
    state.animationSpeed.value = v.intValue()
    speedValLbl.text = s"${v.intValue()}ms"
  }

  private val sizeRow = new HBox(8)
  sizeRow.alignment = Pos.CenterLeft
  HBox.setHgrow(sizeSlider, Priority.Always)
  sizeRow.children.addAll(sizeSlider.delegate, sizeValLbl.delegate)

  private val speedRow = new HBox(8)
  speedRow.alignment = Pos.CenterLeft
  HBox.setHgrow(speedSlider, Priority.Always)
  speedRow.children.addAll(speedSlider.delegate, speedValLbl.delegate)

  // ── Buttons ───────────────────────────────────────────────────
  private val btnStart    = makeBtn("▶  START",     Theme.buttonPrimary)
  private val btnPause    = makeBtn("⏸  PAUSE",     Theme.buttonSecondary)
  private val btnReset    = makeBtn("↺  RESET",     Theme.buttonSecondary)
  private val btnGenerate = makeBtn("⟳  NEW ARRAY", Theme.buttonSecondary)
  btnPause.disable = true

  btnStart.onAction    = _ => if !state.isRunning.value then viz.startSort()
  btnPause.onAction    = _ =>
    if state.isPaused.value then { viz.resumeSort(); btnPause.text = "⏸  PAUSE" }
    else                         { viz.pauseSort();  btnPause.text = "▶  RESUME" }
  btnReset.onAction    = _ => { viz.resetArray(); btnPause.text = "⏸  PAUSE" }
  btnGenerate.onAction = _ => if !state.isRunning.value then viz.resetArray()

  state.isRunning.onChange { (_, _, running) =>
    btnStart.disable    = running
    btnGenerate.disable = running
    genBox.disable      = running
    sizeSlider.disable  = running
    btnPause.disable    = !running
    if !running then btnPause.text = "⏸  PAUSE"
  }

  private val btnRow = new HBox(4)
  HBox.setHgrow(btnReset,    Priority.Always)
  HBox.setHgrow(btnGenerate, Priority.Always)
  btnRow.children.addAll(btnReset.delegate, btnGenerate.delegate)

  // ── Status ────────────────────────────────────────────────────
  private val statusLbl = new Label("READY")
  statusLbl.style    = Theme.labelStyle(10, Theme.TextDim)
  statusLbl.maxWidth = Double.MaxValue
  statusLbl.wrapText = true
  state.statusMessage.onChange { (_, _, v) =>
    val s = v
    statusLbl.text = s.toUpperCase
    statusLbl.style = Theme.labelStyle(10,
      if s.startsWith("Done")        then Theme.AccentSuccess
      else if s.startsWith("Running") then Theme.AccentPrimary
      else if s == "Stopped"          then Theme.AccentDanger
      else Theme.TextDim
    )
  }

  // ── Live sort metrics ────────────────────────────────────────
  private val compLbl = makeStatVal(Theme.AccentPrimary)
  private val swapLbl = makeStatVal(Theme.AccentSecondary)
  private val timeLbl = makeStatVal(Theme.TextBright)

  state.comparisons.onChange { (_, _, v) => compLbl.text = f"${v.longValue()}%,d" }
  state.swaps.onChange       { (_, _, v) => swapLbl.text = f"${v.longValue()}%,d" }
  state.elapsedMs.onChange   { (_, _, v) => timeLbl.text = s"${v}ms" }

  // ── Live JVM metrics ─────────────────────────────────────────
  private val heapLbl   = makeStatVal(Theme.AccentPrimary)
  private val cpuLbl    = makeStatVal(Theme.AccentSecondary)
  private val gcRunsLbl = makeStatVal(Theme.TextBright)
  private val gcTimeLbl = makeStatVal(Theme.TextBright)

  private val jvmTimer = new Timeline:
    cycleCount = Animation.Indefinite
    keyFrames = Seq(
      KeyFrame(Duration(1500), onFinished = _ => {
        heapLbl.text   = f"${SystemMonitor.heapUsedMb}%.1f / ${SystemMonitor.heapMaxMb}%.0f MB"
        cpuLbl.text    = f"${SystemMonitor.cpuLoadPercent}%.1f %%"
        gcRunsLbl.text = SystemMonitor.totalGcCollections.toString
        gcTimeLbl.text = s"${SystemMonitor.totalGcTimeMs}ms"
      })
    )
  jvmTimer.play()

  // ── Panel assembly ────────────────────────────────────────────
  val panel: VBox = new VBox(0)
  panel.prefWidth = 230
  panel.minWidth  = 230
  panel.maxWidth  = 230
  panel.style     = s"${Theme.panelStyle()} -fx-border-color: ${Theme.BgBorder}; " +
    s"-fx-border-width: 0 1 0 0;"
  panel.padding   = Insets(14, 12, 14, 12)

  private val algoHdr  = hdr("ALGORITHM")
  private val genHdr   = hdr("DATA PATTERN")
  private val sizeHdr  = hdr("ARRAY SIZE")
  private val spdHdr   = hdr("STEP DELAY")
  private val sortHdr  = hdr("SORT METRICS")
  private val jvmHdr   = hdr("JVM STATS")

  VBox.setMargin(genHdr,  Insets(10, 0, 3, 0))
  VBox.setMargin(sizeHdr, Insets(10, 0, 3, 0))
  VBox.setMargin(spdHdr,  Insets(8,  0, 3, 0))
  VBox.setMargin(sortHdr, Insets(0,  0, 4, 0))
  VBox.setMargin(jvmHdr,  Insets(0,  0, 4, 0))

  panel.children.addAll(
    // Controls section
    algoHdr.delegate,
    algoBox.delegate,
    genHdr.delegate,
    genBox.delegate,
    sizeHdr.delegate,
    sizeRow.delegate,
    spdHdr.delegate,
    speedRow.delegate,
    spacer(8),
    btnStart.delegate,
    spacer(4),
    btnPause.delegate,
    spacer(4),
    btnRow.delegate,
    spacer(),
    statusLbl.delegate,
    divider(),
    // Sort metrics
    sortHdr.delegate,
    spacer(4),
    statRow("COMPARISONS", compLbl).delegate,
    spacer(3),
    statRow("SWAPS/WRITES", swapLbl).delegate,
    spacer(3),
    statRow("ELAPSED", timeLbl).delegate,
    divider(),
    // JVM stats
    jvmHdr.delegate,
    spacer(4),
    statRow("HEAP", heapLbl).delegate,
    spacer(3),
    statRow("CPU", cpuLbl).delegate,
    spacer(3),
    statRow("GC RUNS", gcRunsLbl).delegate,
    spacer(3),
    statRow("GC TIME", gcTimeLbl).delegate,
    vGrow()
  )
