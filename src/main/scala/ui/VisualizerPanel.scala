package ui

import app.AppState
import engine.AnimationEngine
import model.ArrayGenerator
import scalafx.scene.canvas.{Canvas, GraphicsContext}
import scalafx.scene.paint.Color

class VisualizerPanel(state: AppState):

  private var array: Array[Int]       = generateArray()
  private var cachedMax: Double       = array.max.toDouble
  private var highlightA: Option[Int] = None
  private var highlightB: Option[Int] = None
  private val sortedSet               = collection.mutable.Set.empty[Int]

  val canvas = new Canvas(10, 10)
  val gc: GraphicsContext = canvas.graphicsContext2D

  private val realEngine: AnimationEngine = new AnimationEngine(
    state,
    onArrayChanged = indices => {
      val i = indices(0); val j = indices(1)
      val tmp = array(i); array(i) = array(j); array(j) = tmp
      forceRedraw()
    },
    onHighlight = (a, b) => { highlightA = a; highlightB = b; forceRedraw() },
    onSorted    = idx   => { sortedSet += idx; forceRedraw() },
    onSet       = (idx, value) => {
      array(idx) = value
      if value.toDouble > cachedMax then cachedMax = value.toDouble
      forceRedraw()
    },
    onDone      = () => {
      sortedSet.clear()
      sortedSet ++= array.indices
      highlightA = None; highlightB = None
      forceRedraw()
      state.statusMessage.value =
        s"Done! ${state.comparisons.value} comparisons, ${state.swaps.value} swaps"
      state.isRunning.value = false
    }
  )

  def startSort(): Unit =
    sortedSet.clear(); highlightA = None; highlightB = None
    state.statusMessage.value = s"Running ${state.selectedAlgorithm.value.label}…"
    realEngine.start(array.clone())

  def pauseSort(): Unit  = realEngine.pause()
  def resumeSort(): Unit = realEngine.resume()

  def stopSort(): Unit =
    realEngine.stop(); state.statusMessage.value = "Stopped"

  def resetArray(): Unit =
    realEngine.stop()
    array = generateArray()
    cachedMax = array.max.toDouble
    sortedSet.clear(); highlightA = None; highlightB = None
    state.comparisons.value = 0; state.swaps.value = 0; state.elapsedMs.value = 0
    state.statusMessage.value = "Ready"
    forceRedraw()

  def forceRedraw(): Unit =
    val w = canvas.width.value
    val h = canvas.height.value
    if w < 10 || h < 10 || w > 16384 || h > 16384 then return

    // Background with subtle grid
    gc.fill = Color.web(Theme.BgDeep)
    gc.fillRect(0, 0, w, h)

    // Subtle horizontal grid lines
    gc.stroke = Color.web(Theme.BgBorder)
    gc.lineWidth = 0.5
    val gridLines = 5
    for i <- 1 until gridLines do
      val y = h / gridLines * i
      gc.strokeLine(0, y, w, y)

    if array.isEmpty then return
    val n       = array.length
    val barW    = (w / n).max(1)
    val maxV    = cachedMax
    val gap     = if n <= 100 then 1.0 else 0.0

    for i <- array.indices do
      val barH = (array(i) / maxV * (h - 2)).max(2)
      val x    = i * barW
      val y    = h - barH

      val color =
        if sortedSet.contains(i)       then Theme.AccentSuccess
        else if highlightA.contains(i) then Theme.AccentPrimary
        else if highlightB.contains(i) then Theme.AccentSecondary
        else                                Theme.AccentMuted

      // Bar fill
      gc.fill = Color.web(color)
      gc.fillRect(x, y, barW - gap, barH)

      // Top highlight cap for active bars
      if highlightA.contains(i) || highlightB.contains(i) then
        gc.fill = Color.web("#ffffff33")
        gc.fillRect(x, y, barW - gap, 2)

  private def generateArray(): Array[Int] =
    ArrayGenerator.generate(state.selectedGenerator.value, state.arraySize.value)