package ui.benchmarking

import model.GeneratorType
import ui.utils.{Theme, AlgorithmType}
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.*
import scalafx.scene.layout.*
import scalafx.Includes.*

/** Left-sidebar config panel with Pure / Full mode toggle.
 *  In Pure mode: heap/GC/CPU probe options are hidden.
 *  In Full mode: probe toggles are shown.
 *  Communicates outward only through callbacks. */
class BenchmarkConfigPanel(
                            controller:  BenchmarkController,
                            onRunStart:  () => Unit,
                            onRunFinish: () => Unit,
                            onProgress:  (String, Double) => Unit
                          ):

  // ── Mode toggle ───────────────────────────────────────────
  private val pureRadio = new RadioButton("Pure  (accurate timing)")
  private val fullRadio = new RadioButton("Full  (rich metrics)")
  private val modeGroup = new ToggleGroup

  pureRadio.toggleGroup = modeGroup
  fullRadio.toggleGroup = modeGroup
  pureRadio.selected    = true

  pureRadio.style = Theme.labelStyle(11, Theme.TextBright)
  fullRadio.style = Theme.labelStyle(11, Theme.TextNormal)

  def selectedMode: BenchmarkMode =
    if pureRadio.selected.value then BenchmarkMode.Pure else BenchmarkMode.Full

  // ── Checkbox groups ───────────────────────────────────────
  val algoChecks: Map[AlgorithmType, CheckBox] =
    AlgorithmType.values.map { a =>
      val cb = new CheckBox(a.label); cb.selected = true
      cb.style = Theme.labelStyle(11, Theme.TextNormal)
      a -> cb
    }.toMap

  val genChecks: Map[GeneratorType, CheckBox] =
    GeneratorType.values.map { g =>
      val cb = new CheckBox(g.label)
      cb.selected = Seq(GeneratorType.Random, GeneratorType.Sorted,
        GeneratorType.SortedReverse, GeneratorType.NearlySorted).contains(g)
      cb.style = Theme.labelStyle(11, Theme.TextNormal)
      g -> cb
    }.toMap

  val sizeChecks: Map[Int, CheckBox] =
    Seq(100, 500, 1000, 5000, 10000, 100000).map { n =>
      val cb = new CheckBox(n.toString); cb.selected = n <= 1000
      cb.style = Theme.labelStyle(11, Theme.TextNormal)
      n -> cb
    }.toMap

  // ── Options ───────────────────────────────────────────────
  val warmupCheck = new CheckBox("JIT Warmup before measure")
  warmupCheck.selected = true
  warmupCheck.style = Theme.labelStyle(11, Theme.TextNormal)

  val validateFirstCheck = new CheckBox("Validate correctness before run")
  validateFirstCheck.selected = true
  validateFirstCheck.style = Theme.labelStyle(11, Theme.TextNormal)

  // ── Full-mode probe toggles (hidden in Pure mode) ─────────
  val probeHeapCheck = new CheckBox("Probe heap / alloc rate")
  probeHeapCheck.selected = true
  probeHeapCheck.style = Theme.labelStyle(11, Theme.TextNormal)

  val probeCpuCheck = new CheckBox("Probe CPU thread time")
  probeCpuCheck.selected = true
  probeCpuCheck.style = Theme.labelStyle(11, Theme.TextNormal)

  val probeGcCheck = new CheckBox("Probe GC collections")
  probeGcCheck.selected = true
  probeGcCheck.style = Theme.labelStyle(11, Theme.TextNormal)

  private val probeBox = new VBox(4)
  probeBox.padding = Insets(4, 0, 0, 8)
  probeBox.children.addAll(probeHeapCheck, probeCpuCheck, probeGcCheck)

  // Show/hide probe options based on selected mode
  private def updateProbeVisibility(): Unit =
    val isFull = fullRadio.selected.value
    probeBox.visible   = isFull
    probeBox.managed   = isFull
    pureRadio.style = Theme.labelStyle(11, if pureRadio.selected.value then Theme.AccentPrimary else Theme.TextNormal)
    fullRadio.style = Theme.labelStyle(11, if fullRadio.selected.value then Theme.AccentPrimary else Theme.TextNormal)

  modeGroup.selectedToggle.onChange { (_, _, _) => updateProbeVisibility() }
  updateProbeVisibility()

  // ── Round count sliders ───────────────────────────────────
  private val warmupSlider  = new Slider(500, 5000, 2000)
  private val measureSlider = new Slider(10, 200, 50)
  warmupSlider.showTickLabels  = false; warmupSlider.style = Theme.sliderStyle
  measureSlider.showTickLabels = false; measureSlider.style = Theme.sliderStyle

  private val warmupLbl  = new Label("2000"); warmupLbl.style  = Theme.labelStyle(10, Theme.AccentPrimary)
  private val measureLbl = new Label("50");   measureLbl.style = Theme.labelStyle(10, Theme.AccentPrimary)
  warmupSlider.value.onChange  { (_, _, v) => warmupLbl.text  = v.intValue().toString }
  measureSlider.value.onChange { (_, _, v) => measureLbl.text = v.intValue().toString }

  def warmupRounds:  Int = warmupSlider.value.value.toInt
  def measureRounds: Int = measureSlider.value.value.toInt

  // ── Progress ──────────────────────────────────────────────
  private val progressBar = new ProgressBar:
    progress = 0.0; prefWidth = Double.MaxValue; maxWidth = Double.MaxValue
    style = s"-fx-accent: ${Theme.AccentPrimary};"
    visible = false

  val progressLabel: Label = new Label("Configure options and click RUN"):
    style = Theme.labelStyle(10, Theme.TextDim)

  def setProgress(msg: String, frac: Double): Unit =
    progressLabel.text = msg; progressBar.progress = frac
  def setProgressVisible(v: Boolean): Unit = progressBar.visible = v

  // ── Buttons ───────────────────────────────────────────────
  private val btnRun      = new Button("▶  RUN BENCHMARK"):
    style = Theme.buttonPrimary; maxWidth = Double.MaxValue
  private val btnClear    = new Button("✕  CLEAR"):
    style = Theme.buttonSecondary; maxWidth = Double.MaxValue
  private val btnValidate = new Button("✓  VALIDATE ONLY"):
    style = Theme.buttonSecondary; maxWidth = Double.MaxValue
  private val btnExcelBtn = new Button("⬇  EXCEL"):
    style = Theme.buttonSecondary; maxWidth = Double.MaxValue; disable = true
  private val btnJsonBtn  = new Button("⬇  JSON"):
    style = Theme.buttonSecondary; maxWidth = Double.MaxValue; disable = true

  controller.results.onChange { (_, _) =>
    val has = controller.results.nonEmpty
    btnExcelBtn.disable = !has; btnJsonBtn.disable = !has
  }

  btnClear.onAction    = _ => controller.clearResults()
  btnExcelBtn.onAction = _ => controller.exportExcel()
  btnJsonBtn.onAction  = _ => controller.exportJson()

  btnValidate.onAction = _ =>
    btnValidate.disable = true
    controller.validate { (passed, failed) =>
      btnValidate.disable = false
      progressLabel.style = Theme.labelStyle(10,
        if failed == 0 then Theme.AccentSuccess else Theme.AccentDanger)
    }

  btnRun.onAction = _ =>
    val algos = algoChecks.filter(_._2.selected.value).keys.toList
    val gens  = genChecks.filter(_._2.selected.value).keys.toList
    val sizes = sizeChecks.filter(_._2.selected.value).keys.toList.sorted
    setProgressVisible(true)
    onRunStart()
    controller.runBenchmarks(
      algos         = algos,
      gens          = gens,
      sizes         = sizes,
      mode          = selectedMode,
      validateFirst = validateFirstCheck.selected.value,
      probeHeap     = probeHeapCheck.selected.value,
      probeCpu      = probeCpuCheck.selected.value,
      probeGc       = probeGcCheck.selected.value,
      warmupRounds  = warmupRounds,
      measureRounds = measureRounds,
      onDone        = _ => { setProgressVisible(false); onRunFinish() }
    )

  def setRunning(running: Boolean): Unit =
    btnRun.disable = running; btnClear.disable = running

  // ── Layout helpers ────────────────────────────────────────
  private def hdr(t: String)    = { val l = new Label(t); l.style = Theme.titleStyle(9); l }
  private def spacer(h: Int = 6) = { val r = new Region; r.prefHeight = h; r }
  private def divider() =
    val r = new Region; r.prefHeight = 1; r.maxWidth = Double.MaxValue
    r.style = s"-fx-background-color: ${Theme.BgBorder};"
    VBox.setMargin(r, Insets(10, 0, 10, 0)); r
  private def sliderRow(s: Slider, l: Label) =
    val r = new HBox(8); r.alignment = Pos.CenterLeft
    HBox.setHgrow(s, Priority.Always); r.children.addAll(s, l); r
  private def quickBtn(label: String, color: String, action: => Unit) =
    val b = new Button(label)
    b.style = s"-fx-background-color: transparent; -fx-text-fill: $color; " +
      s"-fx-font-size: 9px; -fx-font-family: 'Consolas', monospace; -fx-cursor: hand; -fx-padding: 0 4;"
    b.onAction = _ => action; b
  private def section(title: String, checks: Iterable[CheckBox]) =
    val sep = new Label("|"); sep.style = Theme.labelStyle(9, Theme.TextDim)
    val row = new HBox(4); row.alignment = Pos.CenterLeft
    row.children.addAll(hdr(title),
      quickBtn("all",  Theme.AccentPrimary, checks.foreach(_.selected = true)),
      sep,
      quickBtn("none", Theme.TextDim,       checks.foreach(_.selected = false)))
    val flow = new FlowPane(6, 5); flow.padding = Insets(4, 0, 0, 0)
    checks.foreach(cb => flow.children.add(cb))
    val box = new VBox(3); box.children.addAll(row, flow); box

  // ── Public node ───────────────────────────────────────────
  val node: VBox =
    val modeRow = new HBox(16); modeRow.alignment = Pos.CenterLeft
    modeRow.children.addAll(pureRadio, fullRadio)

    val exportRow = new HBox(4)
    HBox.setHgrow(btnExcelBtn, Priority.Always); HBox.setHgrow(btnJsonBtn, Priority.Always)
    exportRow.children.addAll(btnExcelBtn, btnJsonBtn)

    val grow = new Region; VBox.setVgrow(grow, Priority.Always)

    val panel = new VBox(0)
    panel.prefWidth = 260; panel.minWidth = 260; panel.maxWidth = 260
    panel.style = s"-fx-background-color: ${Theme.BgBase}; " +
      s"-fx-border-color: ${Theme.BgBorder}; -fx-border-width: 0 1 0 0;"
    panel.padding = Insets(14, 12, 14, 12)

    panel.children.addAll(
      hdr("MODE"),
      spacer(4),
      modeRow,
      spacer(6),
      probeBox,
      divider(),
      section("ALGORITHMS",    algoChecks.values),
      spacer(10),
      section("DATA PATTERNS", genChecks.values),
      spacer(10),
      section("ARRAY SIZES",   sizeChecks.values.toSeq.sortBy(_.text.value.toInt)),
      spacer(10),
      hdr("ROUNDS"),
      spacer(4),
      new Label("Warmup") { style = Theme.labelStyle(9, Theme.TextDim) },
      sliderRow(warmupSlider,  warmupLbl),
      spacer(3),
      new Label("Measure") { style = Theme.labelStyle(9, Theme.TextDim) },
      sliderRow(measureSlider, measureLbl),
      spacer(10),
      hdr("OPTIONS"),
      spacer(4),
      warmupCheck,
      validateFirstCheck,
      grow,
      divider(),
      progressBar,
      spacer(4),
      progressLabel,
      spacer(8),
      btnRun,
      spacer(4),
      btnValidate,
      spacer(4),
      btnClear,
      spacer(4),
      exportRow
    )
    panel