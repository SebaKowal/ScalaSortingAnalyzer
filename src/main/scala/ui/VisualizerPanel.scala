package ui

import app.AppState
import engine.AnimationEngine
import model.{ArrayGenerator, GeneratorType}
import scalafx.scene.canvas.Canvas
import scalafx.scene.paint.Color

class VisualizerPanel(state: AppState):

  private var array: Array[Int]       = generateArray()
  private var highlightA: Option[Int] = None
  private var highlightB: Option[Int] = None
  private val sortedSet               = collection.mutable.Set.empty[Int]

  val canvas = new Canvas(800, 500)
  private val gc     = canvas.graphicsContext2D

  private val realEngine: AnimationEngine = new AnimationEngine(
    state,

    onArrayChanged = indices => {
      val i = indices(0)
      val j = indices(1)
      val tmp = array(i); array(i) = array(j); array(j) = tmp
      redraw()
    },

    onHighlight = (a, b) => {
      highlightA = a
      highlightB = b
      redraw()
    },

    onSorted = idx => {
      sortedSet += idx
      redraw()
    },

    onSet = (idx, value) => {
      array(idx) = value
      redraw()
    },

    onDone = () => {
      sortedSet.clear()
      sortedSet ++= array.indices
      highlightA = None
      highlightB = None
      redraw()
      state.statusMessage.value =
        s"Done! Comparisons: ${state.comparisons.value}  Swaps: ${state.swaps.value}"
      state.isRunning.value = false
    }
  )

  def startSort(): Unit =
    sortedSet.clear()
    highlightA = None
    highlightB = None
    state.statusMessage.value = s"Running ${state.selectedAlgorithm.value.label}…"
    realEngine.start(array.clone())

  def pauseSort(): Unit  = realEngine.pause()
  def resumeSort(): Unit = realEngine.resume()

  def stopSort(): Unit =
    realEngine.stop()
    state.statusMessage.value = "Stopped"

  def resetArray(): Unit =
    realEngine.stop()
    array = generateArray()
    sortedSet.clear()
    highlightA = None
    highlightB = None
    state.comparisons.value   = 0
    state.swaps.value         = 0
    state.elapsedMs.value     = 0
    state.statusMessage.value = "Ready"
    redraw()

  private def redraw(): Unit =
    val w = canvas.width.value
    val h = canvas.height.value
    gc.fill = Color.web("#0f111a")
    gc.fillRect(0, 0, w, h)

    if array.isEmpty then return
    val n    = array.length
    val barW = (w / n).max(1)
    val maxV = array.max.toDouble

    for i <- array.indices do
      val barH = (array(i) / maxV * (h - 4)).max(1)
      val color =
        if sortedSet.contains(i)       then Color.web("#00e676")
        else if highlightA.contains(i) then Color.web("#ff1744")
        else if highlightB.contains(i) then Color.web("#ff9800")
        else                                Color.web("#5c6bc0")
      gc.fill = color
      gc.fillRect(i * barW, h - barH, barW - 1, barH)

  private def generateArray(): Array[Int] =
    ArrayGenerator.generate(state.selectedGenerator.value, state.arraySize.value)

  redraw()