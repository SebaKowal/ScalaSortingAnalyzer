package ui

import app.AppState
import model.{AlgorithmType, GeneratorType}
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.*

class ControlPanel(state: AppState, viz: VisualizerPanel):

  private val algoBox = new ComboBox[String]:
    items = scalafx.collections.ObservableBuffer(AlgorithmType.values.map(_.label).toSeq*)
    value = state.selectedAlgorithm.value.label
    prefWidth = 220

  algoBox.onAction = _ =>
    AlgorithmType.values.find(_.label == algoBox.value.value).foreach { chosen =>
      state.selectedAlgorithm.value = chosen
    }

  private val genBox = new ComboBox[String]:
    items = scalafx.collections.ObservableBuffer(GeneratorType.values.map(_.label).toSeq*)
    value = state.selectedGenerator.value.label
    prefWidth = 220

  genBox.onAction = _ =>
    GeneratorType.values.find(_.label == genBox.value.value).foreach { chosen =>
      state.selectedGenerator.value = chosen
    }

  private val sizeSlider = new Slider(10, 200, state.arraySize.value):
    showTickLabels = true
    showTickMarks  = true
    majorTickUnit  = 50
    blockIncrement = 10

  sizeSlider.value.onChange { (_, _, nv) =>
    state.arraySize.value = nv.intValue()
  }

  private val speedSlider = new Slider(1, 200, state.animationSpeed.value):
    showTickLabels = true
    majorTickUnit  = 50

  speedSlider.value.onChange { (_, _, nv) =>
    state.animationSpeed.value = nv.intValue()
  }

  private val btnStart = new Button("▶  Start"):
    prefWidth = 100
  private val btnPause = new Button("⏸  Pause"):
    prefWidth = 100
    disable = true
  private val btnReset = new Button("↺  Reset"):
    prefWidth = 100
  private val btnGenerate = new Button("⟳  New Array"):
    prefWidth = 100

  btnStart.onAction = _ =>
    if !state.isRunning.value then
      viz.startSort()
      btnStart.disable = true
      btnPause.disable = false

  btnPause.onAction = _ =>
    if state.isPaused.value then
      viz.resumeSort()
      btnPause.text = "⏸  Pause"
    else
      viz.pauseSort()
      btnPause.text = "▶  Resume"

  btnReset.onAction = _ =>
    viz.resetArray()
    btnStart.disable  = false
    btnPause.disable  = true
    btnPause.text     = "⏸  Pause"

  // Generate new array with currently selected generator without resetting stats
  btnGenerate.onAction = _ =>
    if !state.isRunning.value then
      viz.resetArray()
      btnStart.disable = false
      btnPause.disable = true
      btnPause.text    = "⏸  Pause"

  state.isRunning.onChange { (_, _, running) =>
    btnStart.disable   = running
    btnGenerate.disable = running
    genBox.disable     = running
    sizeSlider.disable = running
    btnPause.disable   = !running
    if !running then btnPause.text = "⏸  Pause"
  }

  val panel: VBox = new VBox(14):
    padding  = Insets(16)
    prefWidth = 260
    style    = "-fx-background-color: #1a1d2e;"
    children = Seq(
      sectionLabel("Algorithm"),
      algoBox,
      sectionLabel("Data Pattern"),
      genBox,
      sectionLabel("Array Size"),
      sizeSlider,
      sectionLabel("Speed (ms / step)"),
      speedSlider,
      new Separator,
      new HBox(8, btnStart, btnPause),
      new HBox(8, btnReset, btnGenerate)
    )

  private def sectionLabel(text: String): Label = new Label(text):
    style = "-fx-text-fill: #aaaacc; -fx-font-size: 12px;"