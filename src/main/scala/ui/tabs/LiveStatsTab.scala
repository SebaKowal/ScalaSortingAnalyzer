package ui.tabs

import app.AppState
import benchmark.SystemMonitor
import scalafx.animation.AnimationTimer
import scalafx.geometry.Insets
import scalafx.scene.canvas.Canvas
import scalafx.scene.control.{Label, ScrollPane}
import scalafx.scene.layout.*
import scalafx.scene.paint.Color
import scalafx.Includes.*

class LiveStatsTab(state: AppState):

  private val maxPoints   = 120
  private val heapHistory = collection.mutable.ArrayDeque.empty[Double]
  private val cpuHistory  = collection.mutable.ArrayDeque.empty[Double]

  private def makeValue(init: String = "—"): Label = new Label(init):
    style = "-fx-text-fill: #e0e0ff; -fx-font-size: 18px; -fx-font-weight: bold;"

  private def statCard(title: String, valueLabel: Label, sub: String = ""): VBox =
    val titleLbl = new Label(title):
      style = "-fx-text-fill: #7986cb; -fx-font-size: 10px; -fx-font-weight: bold;"
    val box = new VBox(2)
    box.padding = Insets(12)
    box.prefWidth = 160
    box.style = "-fx-background-color: #1a1d2e; -fx-background-radius: 8; -fx-border-color: #2a2d45; -fx-border-radius: 8;"
    box.children.addAll(titleLbl.delegate, valueLabel.delegate)
    if sub.nonEmpty then
      val subLbl = new Label(sub):
        style = "-fx-text-fill: #5a6080; -fx-font-size: 10px;"
      box.children.add(subLbl.delegate)
    box

  private def makeCanvas(w: Double = 280, h: Double = 60): Canvas = new Canvas(w, h)

  val heapUsedLbl    = makeValue()
  val heapMaxLbl     = makeValue()
  val nonHeapLbl     = makeValue()
  val cpuProcLbl     = makeValue()
  val cpuSysLbl      = makeValue()
  val gcCountLbl     = makeValue()
  val gcTimeLbl      = makeValue()
  val uptimeLbl      = makeValue()
  val processorsLbl  = makeValue(SystemMonitor.availableProcessors.toString)
  val comparisonsLbl = makeValue("0")
  val swapsLbl       = makeValue("0")
  val elapsedLbl     = makeValue("0 ms")

  val heapCanvas = makeCanvas()
  val cpuCanvas  = makeCanvas()

  private def drawSparkline(
                             canvas:  Canvas,
                             history: collection.mutable.ArrayDeque[Double],
                             color:   String,
                             maxVal:  Double
                           ): Unit =
    val gc = canvas.graphicsContext2D
    val w  = canvas.width.value
    val h  = canvas.height.value
    gc.fill = Color.web("#12142b")
    gc.fillRect(0, 0, w, h)
    if history.size < 2 then return
    val pts  = history.toSeq
    val step = w / (maxPoints - 1).toDouble
    gc.stroke = Color.web(color)
    gc.lineWidth = 1.5
    gc.beginPath()
    pts.zipWithIndex.foreach { (v, i) =>
      val x = i * step
      val y = h - (v / maxVal.max(1)) * (h - 4) - 2
      if i == 0 then gc.moveTo(x, y) else gc.lineTo(x, y)
    }
    gc.stroke()
    gc.lineTo((pts.size - 1) * step, h)
    gc.lineTo(0, h)
    gc.closePath()
    gc.fill = Color.web(color + "33")
    gc.fill()

  val gcDetailBox = new VBox(4):
    padding = Insets(8)
    style   = "-fx-background-color: #1a1d2e; -fx-background-radius: 6;"

  private val timer = AnimationTimer { _ =>
    heapUsedLbl.text    = f"${SystemMonitor.heapUsedMb}%.1f MB"
    heapMaxLbl.text     = f"${SystemMonitor.heapMaxMb}%.0f MB"
    nonHeapLbl.text     = f"${SystemMonitor.nonHeapUsedMb}%.1f MB"
    cpuProcLbl.text     = f"${SystemMonitor.cpuLoadPercent}%.1f %%"
    cpuSysLbl.text      = f"${SystemMonitor.systemCpuLoadPercent}%.1f %%"
    gcCountLbl.text     = SystemMonitor.totalGcCollections.toString
    gcTimeLbl.text      = s"${SystemMonitor.totalGcTimeMs} ms"
    uptimeLbl.text      = s"${SystemMonitor.jvmUptime / 1000} s"
    comparisonsLbl.text = f"${state.comparisons.value}%,d"
    swapsLbl.text       = f"${state.swaps.value}%,d"
    elapsedLbl.text     = s"${state.elapsedMs.value} ms"

    heapHistory += SystemMonitor.heapUsedMb
    cpuHistory  += SystemMonitor.cpuLoadPercent
    if heapHistory.size > maxPoints then heapHistory.removeHead()
    if cpuHistory.size  > maxPoints then cpuHistory.removeHead()

    drawSparkline(heapCanvas, heapHistory, "#7986cb", SystemMonitor.heapMaxMb)
    drawSparkline(cpuCanvas,  cpuHistory,  "#ff9800", 100.0)

    val gcRows = SystemMonitor.gcDetails.map { (name, count, time) =>
      new Label(s"$name   runs: $count   time: ${time}ms"):
        style = "-fx-text-fill: #b0b8d8; -fx-font-size: 11px;"
    }
    gcDetailBox.children.setAll(gcRows.map(_.delegate)*)
  }

  timer.start()

  private def sectionTitle(t: String): Label = new Label(t):
    style = "-fx-text-fill: #7986cb; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10 0 4 0;"

  private def sparkLabel(t: String): Label = new Label(t):
    style = "-fx-text-fill: #5a6080; -fx-font-size: 10px; -fx-padding: 0 0 2 2;"

  def build(): ScrollPane =
    val memRow = new FlowPane(10, 10)
    memRow.padding = Insets(4)
    memRow.children.addAll(
      statCard("Heap Used",  heapUsedLbl).delegate,
      statCard("Heap Max",   heapMaxLbl).delegate,
      statCard("Non-Heap",   nonHeapLbl).delegate,
      statCard("Processors", processorsLbl, "logical cores").delegate
    )

    val cpuRow = new FlowPane(10, 10)
    cpuRow.padding = Insets(4)
    cpuRow.children.addAll(
      statCard("CPU (process)", cpuProcLbl).delegate,
      statCard("CPU (system)",  cpuSysLbl).delegate,
      statCard("JVM Uptime",    uptimeLbl).delegate,
      statCard("GC Total Runs", gcCountLbl).delegate
    )

    val sortRow = new FlowPane(10, 10)
    sortRow.padding = Insets(4)
    sortRow.children.addAll(
      statCard("Comparisons",  comparisonsLbl).delegate,
      statCard("Swaps / Sets", swapsLbl).delegate,
      statCard("Elapsed",      elapsedLbl).delegate,
      statCard("GC Time",      gcTimeLbl).delegate
    )

    val innerContent = new VBox(6)
    innerContent.padding = Insets(12)
    innerContent.style   = "-fx-background-color: #0f111a;"
    innerContent.children.addAll(
      sectionTitle("Memory").delegate,
      memRow.delegate,
      sectionTitle("CPU").delegate,
      cpuRow.delegate,
      sectionTitle("Current Sort").delegate,
      sortRow.delegate,
      sectionTitle("Heap Usage (live)").delegate,
      sparkLabel("MB over time").delegate,
      heapCanvas.delegate,
      sectionTitle("CPU Load (live)").delegate,
      sparkLabel("% over time").delegate,
      cpuCanvas.delegate,
      sectionTitle("GC Collectors").delegate,
      gcDetailBox.delegate
    )

    val sp = new ScrollPane
    sp.content    = innerContent
    sp.fitToWidth = true
    sp.style      = "-fx-background: #0f111a; -fx-background-color: #0f111a; -fx-border-color: transparent;"
    sp