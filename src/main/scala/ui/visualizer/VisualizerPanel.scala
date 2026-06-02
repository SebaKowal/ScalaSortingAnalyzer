package ui.visualizer

import scalafx.scene.text.TextAlignment
import app.AppState
import engine.AnimationEngine
import model.ArrayGenerator
import scalafx.Includes.jfxColor2sfx
import scalafx.scene.canvas.{Canvas, GraphicsContext}
import scalafx.scene.layout.VBox
import scalafx.scene.paint.Color
import ui.utils.Theme
import ui.utils.Theme.bucketColors

class VisualizerPanel(state: AppState):

  private var array: Array[Int]       = generateArray()
  private var cachedMax: Double       = array.max.toDouble
  private var highlightA: Option[Int] = None
  private var highlightB: Option[Int] = None
  private val sortedSet               = collection.mutable.Set.empty[Int]

  private var countArray: Map[Int, Int]  = Map.empty
  private var countMin: Int              = 0
  private var countPhase: Int            = 0
  private var placedPositions: Set[Int]  = Set.empty

  private var buckets: Map[Int, List[Int]]       = Map.empty
  private var bucketCount: Int                   = 0
  private var elementBucketMap: Map[Int, Int]    = Map.empty



  def isNonComparative: Boolean =
    val name = state.selectedAlgorithm.value.label
    name == "Counting Sort" || name == "Bucket Sort"

  val topCanvas  = new Canvas(10, 80)
  val mainCanvas = new Canvas(10, 10)

  private val topGc: GraphicsContext  = topCanvas.graphicsContext2D
  private val mainGc: GraphicsContext = mainCanvas.graphicsContext2D

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
      if state.selectedAlgorithm.value.label == "Counting Sort" then
        countPhase = 2
        placedPositions = placedPositions + idx
      redraw()
    },
    onCountUpdate = (idx, value) => {
      if value == -1 then
        if countPhase == 0 then ()
        val key = idx
        val prev = countArray.getOrElse(key, 0)
        countArray = countArray.updated(key, prev + 1)
      else
        val key = idx + countMin
        countArray = countArray.updated(key, value)
        val totalCounts = countArray.values.sum
        if totalCounts > array.length && countPhase < 1 then countPhase = 1
      redraw()
    },
    onBucketUpdate = (bucket, values) => {
      val existing = buckets.getOrElse(bucket, Nil)
      buckets = buckets.updated(bucket, existing ++ values)
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
    countMin = 0; countPhase = 0; placedPositions = Set.empty
    elementBucketMap = Map.empty
    if state.selectedAlgorithm.value.label == "Counting Sort" && array.nonEmpty then
      countMin = array.min
    bucketCount = math.max(1, array.length / 2)
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
    countMin = 0; countPhase = 0; placedPositions = Set.empty
    elementBucketMap = Map.empty
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
    topGc.clearRect(0, 0, w, h)
    topGc.fill = Color.web(Theme.BgDeep)
    topGc.fillRect(0, 0, w, h)
    topGc.setFont(javafx.scene.text.Font.font("Consolas", 10))

    val algo = state.selectedAlgorithm.value.label

    if algo == "Counting Sort" then drawCountingTop(w, h)
    else if algo == "Bucket Sort" then drawBucketTop(w, h)

    topGc.stroke = Color.web(Theme.BgBorder)
    topGc.setLineWidth(1.5)
    topGc.strokeLine(0, h - 1, w, h - 1)

  private def drawCountingTop(w: Double, h: Double): Unit =
      val maxVal = if array.nonEmpty then cachedMax.toInt else 0
      val totalCells = (maxVal - countMin + 1).max(1)
      val cellW = w / totalCells
      val boxY = 26.0
      val boxH = 26.0

      val (phaseLabel, phaseColor) = countPhase match
        case 0 => ("PHASE 1: FREQUENCY COUNT", Theme.AccentPrimary)
        case 1 => ("PHASE 2: PREFIX SUM", Theme.AccentSecondary)
        case 2 => ("PHASE 3: REVERSE PLACING", Theme.AccentSuccess)
        case _ => ("", Theme.TextDim)

      topGc.textAlign = scalafx.scene.text.TextAlignment.Left
      topGc.fill = Color.web(phaseColor)
      topGc.setFont(javafx.scene.text.Font.font("Consolas", javafx.scene.text.FontWeight.BOLD, 10))
      topGc.fillText(phaseLabel, 8, 14)

      val highlightedCells = collection.mutable.Map.empty[Int, String]
      if countPhase == 1 then
        highlightA.foreach(cellIdx => highlightedCells(cellIdx) = Theme.AccentPrimary)
        highlightB.foreach(cellIdx => highlightedCells(cellIdx) = Theme.AccentSecondary)
      else
        highlightA.foreach(idx => if idx < array.length then highlightedCells(array(idx) - countMin) = Theme.AccentPrimary)
        highlightB.foreach(idx => if idx < array.length then highlightedCells(array(idx) - countMin) = Theme.AccentSecondary)

      topGc.textAlign = scalafx.scene.text.TextAlignment.Center

      for i <- 0 until totalCells do
        val value = countMin + i
        val count = countArray.getOrElse(value, 0)
        val x = i * cellW
        val centerX = x + cellW / 2

        val isHighlighted = highlightedCells.contains(i)
        val currentCellColor = if isHighlighted then highlightedCells(i) else phaseColor

        if isHighlighted then
          topGc.fill = Color.web(currentCellColor + "44")
          topGc.fillRect(x + 1, boxY, (cellW - 2).max(1), boxH)
          topGc.stroke = Color.web(currentCellColor)
          topGc.setLineWidth(1.6)
          topGc.strokeRect(x + 1, boxY, (cellW - 2).max(1), boxH)
        else if count > 0 then
          topGc.fill = Color.web(phaseColor + "15")
          topGc.fillRect(x + 1, boxY, (cellW - 2).max(1), boxH)
          topGc.stroke = Color.web(phaseColor + "aa")
          topGc.setLineWidth(1.0)
          topGc.strokeRect(x + 1, boxY, (cellW - 2).max(1), boxH)
        else
          topGc.fill = Color.web(Theme.BgBorder + "0d")
          topGc.fillRect(x + 1, boxY, (cellW - 2).max(1), boxH)
          topGc.stroke = Color.web(Theme.BgBorder + "33")
          topGc.setLineWidth(0.6)
          topGc.strokeRect(x + 1, boxY, (cellW - 2).max(1), boxH)

        if cellW >= 12 then
          topGc.fill = if isHighlighted then Color.web(Theme.TextBright)
          else if count > 0 then Color.web(Theme.TextBright).opacity(0.8)
          else Color.web(Theme.TextDim).opacity(0.3)
          topGc.setFont(javafx.scene.text.Font.font("Consolas", javafx.scene.text.FontWeight.BOLD, 10))
          topGc.fillText(count.toString, centerX, boxY + 17)

          if isHighlighted && cellW >= 24 then
            topGc.setFont(javafx.scene.text.Font.font("Consolas", 7.5))
            if countPhase == 2 then
              topGc.fill = Color.web(Theme.AccentDanger)
              topGc.fillText("-1", centerX + 9, boxY + 10)
            else if countPhase == 0 then
              topGc.fill = Color.web(Theme.AccentSuccess)
              topGc.fillText("+1", centerX + 9, boxY + 10)

          topGc.fill = if isHighlighted then Color.web(currentCellColor) else Color.web(Theme.TextDim)
          topGc.setFont(javafx.scene.text.Font.font("Consolas", 8))
          topGc.fillText(value.toString, centerX, boxY + boxH + 12)

      if countPhase == 1 && highlightA.nonEmpty && highlightB.nonEmpty then
        val idxA = highlightA.head
        val idxB = highlightB.head
        if (idxB - idxA).abs == 1 then
          val cX_A = idxA * cellW + cellW / 2
          val cX_B = idxB * cellW + cellW / 2

          topGc.stroke = Color.web(Theme.AccentSecondary + "cc")
          topGc.setLineWidth(1.5)

          topGc.beginPath()
          topGc.moveTo(cX_A, boxY - 2)
          val controlY = boxY - 15
          topGc.quadraticCurveTo((cX_A + cX_B) / 2, controlY, cX_B, boxY - 2)
          topGc.stroke()

          topGc.fill = Color.web(Theme.AccentSecondary)
          val arrowDir = if cX_B > cX_A then -1.0 else 1.0
          topGc.beginPath()
          topGc.moveTo(cX_B, boxY - 2)
          topGc.lineTo(cX_B + (arrowDir * 3), boxY - 6)
          topGc.lineTo(cX_B + (arrowDir * -1), boxY - 9)
          topGc.closePath()
          topGc.fill()

      if countPhase != 1 then
        val mainW = mainCanvas.width.value
        val n = array.length
        val mainBarW = (mainW / n).max(1.0)

        def drawLink(mainIdx: Int, colorHex: String): Unit =
          if mainIdx >= 0 && mainIdx < array.length then
            val v = array(mainIdx)
            val cellIdx = v - countMin
            if cellIdx >= 0 && cellIdx < totalCells then
              val topCellCenterX = cellIdx * cellW + cellW / 2
              val mainBarCenterX = mainIdx * mainBarW + mainBarW / 2

              topGc.stroke = Color.web(colorHex + "aa")
              topGc.setLineWidth(1.2)
              topGc.strokeLine(topCellCenterX, boxY + boxH + 14, mainBarCenterX, h - 1)

        highlightA.foreach(idx => drawLink(idx, Theme.AccentPrimary))
        highlightB.foreach(idx => drawLink(idx, Theme.AccentSecondary))

  private def barColorFor(phase: Int): String = phase match
    case 0 => Theme.AccentPrimary
    case 1 => Theme.AccentSecondary
    case 2 => Theme.AccentSuccess
    case _ => Theme.AccentMuted

  private def drawBucketTop(w: Double, h: Double): Unit =
    val activeBuckets = buckets.toSeq.filter(_._2.nonEmpty).sortBy(_._1)
    if activeBuckets.isEmpty then return

    val numBuckets    = activeBuckets.size
    val sectionW      = w / numBuckets
    val maxElements   = activeBuckets.map(_._2.size).max.max(1)
    val reservedBot   = 16.0
    val reservedTop   = 4.0
    val barZone       = h - reservedBot - reservedTop

    for ((bucket, values), k) <- activeBuckets.zipWithIndex do
      val count    = values.size
      val x        = k * sectionW
      val barH     = (count.toDouble / maxElements * barZone).max(2.0)
      val y        = h - reservedBot - barH
      val colorHex = bucketColors(bucket % bucketColors.length)

      topGc.fill = Color.web(colorHex + "cc")
      topGc.fillRect(x + 1, y, (sectionW - 2).max(1), barH)

      if sectionW >= 18 then
        topGc.setTextAlign(TextAlignment.Center)
        val centerX = x + sectionW / 2

        topGc.fill = Color.web(Theme.TextBright)
        topGc.setFont(javafx.scene.text.Font.font("Consolas", javafx.scene.text.FontWeight.BOLD, 9))
        val labelY = if barH > 15 then y + 11 else y - 2
        topGc.fillText(s"B$bucket", centerX, labelY)

        topGc.fill = Color.web(colorHex)
        topGc.setFont(javafx.scene.text.Font.font("Consolas", 8))
        topGc.fillText(count.toString, centerX, h - 4)

    topGc.stroke = Color.web(Theme.BgBorder)
    topGc.setLineWidth(1)
    for ((_bucket, _values), k) <- activeBuckets.zipWithIndex do
      if k > 0 then
        val x = k * sectionW
        topGc.strokeLine(x, reservedTop, x, h - reservedBot)

    val mainW    = mainCanvas.width.value
    val n        = array.length
    val mainBarW = (mainW / n).max(1.0)

    var pos = 0
    val spans = collection.mutable.ArrayBuffer[(Int, Int, Int)]()
    for (bucket, values) <- activeBuckets do
      if values.nonEmpty then
        val start = pos
        val end   = pos + values.size - 1
        spans += ((bucket, start, end))
        pos += values.size

    topGc.setLineWidth(1.2)
    for (bucket, startIdx, endIdx) <- spans do
      val colorHex = bucketColors(bucket % bucketColors.length)
      val k        = activeBuckets.indexWhere(_._1 == bucket)
      val topX0    = k * sectionW
      val topX1    = (k + 1) * sectionW

      val mainX0 = startIdx * mainBarW
      val mainX1 = (endIdx + 1) * mainBarW
      val topY = h - 1

      topGc.stroke = Color.web(colorHex + "88")
      topGc.strokeLine(topX0 + 1, topY, mainX0 + 1, topY + 6)
      topGc.strokeLine(topX1 - 1, topY, mainX1 - 1, topY + 6)

  private def drawMain(): Unit =
    val w = mainCanvas.width.value
    val h = mainCanvas.height.value
    mainGc.fill = Color.web(Theme.BgDeep)
    mainGc.fillRect(0, 0, w, h)

    if array.isEmpty then return

    val n      = array.length
    val barW   = (w / n).max(1)
    val maxV   = cachedMax
    val gap    = if n <= 100 then 1.0 else 0.0
    val algo   = state.selectedAlgorithm.value.label

    val bucketColorMap: Map[Int, String] = buildBucketColorMap()

    for i <- array.indices do
      val barH = (array(i) / maxV * (h - 2)).max(2)
      val x    = i * barW
      val y    = h - barH

      val color =
        if sortedSet.contains(i)       then Theme.AccentSuccess
        else if highlightA.contains(i) then Theme.AccentPrimary
        else if highlightB.contains(i) then Theme.AccentSecondary
        else if algo == "Bucket Sort"  then
          bucketColorMap.getOrElse(i, Theme.AccentMuted)
        else if algo == "Counting Sort" && placedPositions.contains(i) then
          Theme.AccentSuccess
        else                                Theme.AccentMuted

      mainGc.fill = Color.web(color)
      mainGc.fillRect(x, y, barW - gap, barH)

      if highlightA.contains(i) || highlightB.contains(i) then
        mainGc.fill = Color.web("#ffffff33")
        mainGc.fillRect(x, y, barW - gap, 2)

  private def buildBucketColorMap(): Map[Int, String] =
    if state.selectedAlgorithm.value.label != "Bucket Sort" || buckets.isEmpty then
      return Map.empty
    val activeBuckets = buckets.toSeq.filter(_._2.nonEmpty).sortBy(_._1)
    var pos = 0
    val result = collection.mutable.Map.empty[Int, String]
    for (bucket, values) <- activeBuckets do
      val colorHex = bucketColors(bucket % bucketColors.length)
      for _ <- values.indices do
        result(pos) = colorHex
        pos += 1
    result.toMap


  private def generateArray(): Array[Int] =
    ArrayGenerator.generate(state.selectedGenerator.value, state.arraySize.value)