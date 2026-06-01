package ui.visualizer

import scalafx.scene.text.TextAlignment
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

  // Counting sort state
  private var countArray: Map[Int, Int]  = Map.empty
  private var countMin: Int              = 0
  private var countPhase: Int            = 0   // 0=counting, 1=prefix, 2=placing
  private var placedPositions: Set[Int]  = Set.empty

  // Bucket sort state
  private var buckets: Map[Int, List[Int]]       = Map.empty
  private var bucketCount: Int                   = 0
  private var elementBucketMap: Map[Int, Int]    = Map.empty // mainArray index -> bucketId

  // Nowa, rozszerzona paleta 12 kolorów (bez standardowego zielonego, pomarańczowego i niebieskiego)
  // Wykorzystuje jaskrawe fiolety, magenta, głębokie róże, neonowe czerwienie i jasne odcienie żółci/złota
  private val bucketColors = Array(
    "#ff2d6b", // Jaskrawy Róż / Magenta
    "#b06aff", // Elektryczny Fiolet
    "#ffe066", // Żywy Żółty
    "#d946ef", // Fuchsia
    "#ff4d4d", // Neonowa Czerwień
    "#a855f7", // Intensywny Fiolet (Orchid)
    "#facc15", // Ciepłe Złoto
    "#ec4899", // Hot Pink
    "#f87171", // Jasny Koral / Łososiowy
    "#c084fc", // Pastelowy Lawendowy
    "#e11d48", // Karminowy Róż
    "#fef08a"  // Pastelowy Żółty
  )

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

  // ─── TOP PANEL ───────────────────────────────────────────────────────────────

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


  // ─── COUNTING SORT TOP ───────────────────────────────────────────────────────

  private def drawCountingTop(w: Double, h: Double): Unit =
    if countArray.isEmpty then return

    val phaseLabel = countPhase match
      case 0 => "PHASE 1: COUNTING"
      case 1 => "PHASE 2: PREFIX SUM"
      case 2 => "PHASE 3: PLACING"
      case _ => ""
    val phaseColor = countPhase match
      case 0 => Theme.AccentPrimary
      case 1 => Theme.AccentSecondary
      case 2 => Theme.AccentSuccess
      case _ => Theme.TextDim
    topGc.fill = Color.web(phaseColor)
    topGc.setFont(javafx.scene.text.Font.font("Consolas", javafx.scene.text.FontWeight.BOLD, 9))
    topGc.fillText(phaseLabel, 6, 12)
    topGc.setFont(javafx.scene.text.Font.font("Consolas", 10))

    val active = countArray.toSeq.filter(_._2 > 0).sortBy(_._1)
    if active.isEmpty then return

    val totalCells = active.size
    val reservedTop = 18.0
    val reservedBot = 18.0
    val barZone = h - reservedTop - reservedBot - 2
    val cellW   = w / totalCells
    val maxVal  = active.map(_._2).max.max(1).toDouble

    for ((value, count), k) <- active.zipWithIndex do
      val x    = k * cellW
      val barH = (count / maxVal * barZone).max(2.0)
      val y    = h - reservedBot - barH

      val barColor = countPhase match
        case 0 => Theme.AccentPrimary
        case 1 => Theme.AccentSecondary
        case 2 => Theme.AccentSuccess
        case _ => Theme.AccentMuted

      topGc.fill = Color.web(barColor)
      topGc.fillRect(x + 1, y, (cellW - 2).max(1), barH)

      if cellW >= 14 then
        topGc.fill = Color.web(Theme.TextBright)
        topGc.setFont(javafx.scene.text.Font.font("Consolas", 9))
        val countStr = count.toString
        val tx = x + cellW / 2 - countStr.length * 3
        topGc.fillText(countStr, tx, y - 2)

      if cellW >= 14 then
        topGc.fill = Color.web(phaseColor)
        topGc.setFont(javafx.scene.text.Font.font("Consolas", 8))
        val valStr = value.toString
        val vx = x + cellW / 2 - valStr.length * 2.5
        topGc.fillText(valStr, vx, h - 4)

    val mainW  = mainCanvas.width.value
    val mainH  = mainCanvas.height.value
    val n      = array.length
    val mainBarW = (mainW / n).max(1.0)

    topGc.setLineWidth(0.5)
    for ((value, count), k) <- active.zipWithIndex do
      if count > 0 then
        val topCellCenterX = k * cellW + cellW / 2

        for i <- array.indices do
          if array(i) == value then
            val mainBarCenterX = i * mainBarW + mainBarW / 2
            val dist = math.abs(topCellCenterX - mainBarCenterX)
            if dist < w * 0.35 then
              topGc.stroke = Color.web(barColorFor(countPhase) + "40")
              topGc.strokeLine(topCellCenterX, h - 2, mainBarCenterX, h + 4)

  private def barColorFor(phase: Int): String = phase match
    case 0 => Theme.AccentPrimary
    case 1 => Theme.AccentSecondary
    case 2 => Theme.AccentSuccess
    case _ => Theme.AccentMuted


  // ─── BUCKET SORT TOP ─────────────────────────────────────────────────────────

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
        // Uruchamiamy automatyczne centrowanie tekstu w poziomie
        topGc.setTextAlign(TextAlignment.Center)
        val centerX = x + sectionW / 2

        // 1. Nazwa kubełka: wsuwa się do środka (y + 11), a jeśli słupek jest za niski, wyskakuje nad niego (y - 2)
        topGc.fill = Color.web(Theme.TextBright)
        topGc.setFont(javafx.scene.text.Font.font("Consolas", javafx.scene.text.FontWeight.BOLD, 9))
        val labelY = if barH > 15 then y + 11 else y - 2
        topGc.fillText(s"B$bucket", centerX, labelY)

        // 2. Licznik elementów: stabilnie wycentrowany na samym dole kolumny
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


  // ─── MAIN PANEL ──────────────────────────────────────────────────────────────

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