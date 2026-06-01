package ui.visualizer

import app.AppState
import engine.AnimationEngine
import model.ArrayGenerator
import scalafx.scene.canvas.{Canvas, GraphicsContext}
import scalafx.scene.layout.VBox
import scalafx.scene.paint.Color
import ui.utils.Theme

class VisualizerPanel(state: AppState):

  private var array: Array[Int]       = generateArray()
  private var cachedMax: Double       = array.max.toDouble
  private var highlightA: Option[Int] = None
  private var highlightB: Option[Int] = None
  private val sortedSet               = collection.mutable.Set.empty[Int]

  private var countArray: Map[Int, Int] = Map.empty
  private var buckets: Map[Int, List[Int]] = Map.empty

  def isNonComparative: Boolean =
    val name = state.selectedAlgorithm.value.label
    name == "Counting Sort" || name == "Bucket Sort"

  val topCanvas  = new Canvas(10, 80)
  val mainCanvas = new Canvas(10, 10)

  val topGc: GraphicsContext  = topCanvas.graphicsContext2D
  val mainGc: GraphicsContext = mainCanvas.graphicsContext2D

  val root = new VBox()
  updateLayout()

  private val realEngine: AnimationEngine = new AnimationEngine(
    state,
    onArrayChanged = indices => {
      val i = indices(0); val j = indices(1)
      val tmp = array(i); array(i) = array(j); array(j) = tmp
      redraw()
    },
    onHighlight = (a, b) => { highlightA = a; highlightB = b; redraw() },
    onSorted    = idx   => { sortedSet += idx; redraw() },
    onSet       = (idx, value) => {
      array(idx) = value
      if value.toDouble > cachedMax then cachedMax = value.toDouble
      redraw()
    },
    onCountUpdate = (idx, value) => {
      countArray = countArray.updated(idx, value)
      redraw()
    },
    onBucketUpdate = (bucket, values) => {
      val updated = buckets.getOrElse(bucket, Nil) ++ values
      buckets = buckets.updated(bucket, updated)
      redraw()
    },
    onDone      = () => {
      sortedSet.clear()
      sortedSet ++= array.indices
      highlightA = None; highlightB = None
      redraw()
      state.statusMessage.value =
        s"Done! ${state.comparisons.value} comparisons, ${state.swaps.value} swaps"
      state.isRunning.value = false
    }
  )

  private def updateLayout(): Unit =
    root.children.clear()
    if isNonComparative then
      root.children.addAll(topCanvas, mainCanvas)
    else
      root.children.add(mainCanvas)

  def startSort(): Unit =
    sortedSet.clear(); highlightA = None; highlightB = None
    countArray = Map.empty; buckets = Map.empty
    updateLayout()
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
    countArray = Map.empty; buckets = Map.empty
    updateLayout()
    state.comparisons.value = 0; state.swaps.value = 0; state.elapsedMs.value = 0
    state.statusMessage.value = "Ready"
    redraw()

  def redraw(): Unit =
    if isNonComparative then
      drawTop()
      drawMain()
    else
      drawMain()

  private def drawTop(): Unit =
    val w = topCanvas.width.value
    val h = topCanvas.height.value

    // Czyszczenie tła
    topGc.clearRect(0, 0, w, h)
    topGc.fill = Color.web(Theme.BgDeep)
    topGc.fillRect(0, 0, w, h)

    val algorithmName = state.selectedAlgorithm.value.label
    topGc.setFont(javafx.scene.text.Font.font("Consolas", 10))

    // --- 1. UPROSZCZONY COUNTING SORT (Tylko aktywne zliczenia) ---
    if countArray.nonEmpty && algorithmName == "Counting Sort" then
      // Filtrujemy tylko wartości, które mają licznik większy niż 0
      val activeCounts = countArray.toSeq.filter(_._2 > 0).sortBy(_._1)

      if activeCounts.nonEmpty then
        val totalElements = activeCounts.size
        val cellW = w / totalElements
        val maxCount = activeCounts.map(_._2).max.max(1)

        for ((idx, value), k) <- activeCounts.zipWithIndex do
          val x = k * cellW
          // Rezerwujemy 35px od góry, żeby liczby nad słupkami nigdy nie uciekały
          val barH = (value.toDouble / maxCount) * (h - 35).max(10)
          val y = h - barH - 2

          // Rysowanie słupka wartości
          topGc.fill = Color.web(Theme.AccentPrimary)
          topGc.fillRect(x, y, (cellW - 1).max(1.0), barH)

          // Wartość nad słupkiem (widoczna bez ucinania)
          if cellW > 16 then
            topGc.fill = Color.web(Theme.TextBright)
            topGc.fillText(value.toString, x + (cellW / 2) - 4, y - 5)

            // Jaki to numerek (indeks) – rysowany wewnątrz słupka na dole
            topGc.fill = Color.web(Theme.BgDeep)
            topGc.fillText(idx.toString, x + (cellW / 2) - 4, h - 6)

    // --- 2. BUCKET SORT (Przylegające kolumny + napisy na górze) ---
    if buckets.nonEmpty && algorithmName == "Bucket Sort" then
      // Filtrujemy: bierzemy TYLKO te kubełki, które nie są puste
      val activeBuckets = buckets.toSeq.filter(_._2.nonEmpty).sortBy(_._1)

      if activeBuckets.nonEmpty then
        val numBuckets = activeBuckets.size
        val sectionW = w / numBuckets
        val maxElementsInBucket = activeBuckets.map(_._2.size).max.max(1)

        for ((bucket, values), k) <- activeBuckets.zipWithIndex do
          val count = values.size
          val x = k * sectionW

          // Bezpieczna wysokość (zabezpieczenie przed navbar)
          val barH = (count.toDouble / maxElementsInBucket) * (h - 35).max(10)
          val y = h - barH - 2

          // Słupki przylegają do siebie (brak przerw między kolumnami)
          topGc.fill = Color.web(Theme.AccentMuted)
          topGc.fillRect(x, y, sectionW.max(1.0), barH)

          // Liczba elementów nad słupkiem
          if sectionW > 16 then
            topGc.fill = Color.web(Theme.TextBright)
            topGc.fillText(count.toString, x + (sectionW / 2) - 4, y - 5)

        // --- SKRAJNE NAPISY JAKO FLEX NAKŁADKA NA GÓRZE ---
        topGc.fill = Color.web(Theme.TextBright)

        // Pierwszy aktywny kubełek (np. B0) po lewej stronie na samej górze
        val firstBucketStr = s"B${activeBuckets.head._1}"
        topGc.fillText(firstBucketStr, 4, 15)

        // Ostatni aktywny kubełek po prawej stronie na samej górze
        val lastBucketStr = s"B${activeBuckets.last._1}"
        val approxTextWidth = lastBucketStr.length * 6
        topGc.fillText(lastBucketStr, w - approxTextWidth - 4, 15)

    // --- LINIA ODCIĘCIA GÓRNEGO PANELU OD DOLNEGO ---
    topGc.stroke = Color.web(Theme.BgBorder)
    topGc.setLineWidth(1.5)
    topGc.strokeLine(0, h - 1, w, h - 1)


  private def drawMain(): Unit =
    val w = mainCanvas.width.value
    val h = mainCanvas.height.value
    mainGc.fill = Color.web(Theme.BgDeep)
    mainGc.fillRect(0, 0, w, h)

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

      mainGc.fill = Color.web(color)
      mainGc.fillRect(x, y, barW - gap, barH)

      if highlightA.contains(i) || highlightB.contains(i) then
        mainGc.fill = Color.web("#ffffff33")
        mainGc.fillRect(x, y, barW - gap, 2)

  private def generateArray(): Array[Int] =
    ArrayGenerator.generate(state.selectedGenerator.value, state.arraySize.value)
