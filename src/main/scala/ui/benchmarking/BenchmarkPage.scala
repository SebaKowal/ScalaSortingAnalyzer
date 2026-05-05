package ui.benchmarking

import benchmark.full.FullResult
import benchmark.pure.PureResult
import ui.utils.Theme
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.Label
import scalafx.scene.layout.*
import scalafx.scene.Node
import scalafx.Includes.*

import scala.concurrent.ExecutionContext

object BenchmarkPage:

  private val benchmarkExecutor =
    java.util.concurrent.Executors.newSingleThreadExecutor { r =>
      val t = new Thread(r, "benchmark-worker"); t.setDaemon(true); t
    }
  given ExecutionContext = ExecutionContext.fromExecutorService(benchmarkExecutor)

  def build(results: ObservableBuffer[AnyRef]): BorderPane =

    val controller = new BenchmarkController(
      results       = results,
      onStateChange = _ => (),
      onResults     = items => results.addAll(items.map(_.asInstanceOf[AnyRef])*)
    )

    val table   = new BenchmarkResultsTable(results, controller)
    val summary = new BenchmarkSummaryBar(results)

    lazy val configPanel: BenchmarkConfigPanel = new BenchmarkConfigPanel(
      controller  = controller,
      onRunStart  = () => configPanel.setRunning(true),
      onRunFinish = () => configPanel.setRunning(false),
      onProgress  = (msg, frac) => configPanel.setProgress(msg, frac)
    )

    configPanel.node

    val tableBox = new VBox(0):
      style = s"-fx-background-color: ${Theme.BgDeep};"
      hgrow = Priority.Always
      vgrow = Priority.Always
      children.setAll(table.tableView, summary.node)

    val runPage = new HBox:
      style = s"-fx-background-color: ${Theme.BgDeep};"
      vgrow = Priority.Always
      children.setAll(configPanel.node, tableBox)

    val analysisPage = BenchmarkAnalysisPage.build(results)

    val pageArea = new StackPane:
      style = s"-fx-background-color: ${Theme.BgDeep};"
      vgrow = Priority.Always

    def makeTab(labelText: String): (VBox, Label, Region) =
      val lbl = new Label(labelText):
        style = Theme.labelStyle(11, Theme.TextDim)
        padding = Insets(0, 6, 0, 6)

      // Region jest teraz poprawnie dostępny dzięki importowi scalafx.scene.layout.*
      val ind = new Region:
        prefHeight = 2
        maxWidth = Double.MaxValue
        style = "-fx-background-color: transparent;"

      val tab = new VBox(0):
        alignment = Pos.Center
        prefHeight = 36
        padding = Insets(0, 14, 0, 14)
        style = "-fx-cursor: hand;"
        children.setAll(lbl, ind)

      (tab, lbl, ind)

    val (runTab, runLbl, runInd)           = makeTab("⚙  RUN")
    val (analysisTab, analysisLbl, anaInd) = makeTab("📈  ANALYSIS")

    def activate(aTab: VBox, aLbl: Label, aInd: Region, iTab: VBox, iLbl: Label, iInd: Region): Unit =
      aLbl.style = Theme.labelStyle(11, Theme.AccentPrimary)
      aInd.style = s"-fx-background-color: ${Theme.AccentPrimary};"
      aTab.style = s"-fx-cursor: hand; -fx-background-color: ${Theme.BgBase};"
      iLbl.style = Theme.labelStyle(11, Theme.TextDim)
      iInd.style = "-fx-background-color: transparent;"
      iTab.style = "-fx-cursor: hand;"

    def showRun(): Unit =
      pageArea.children.setAll(runPage)
      table.setMode(configPanel.selectedMode)
      activate(runTab, runLbl, runInd, analysisTab, analysisLbl, anaInd)

    def showAnalysis(): Unit =
      pageArea.children.setAll(analysisPage)
      activate(analysisTab, analysisLbl, anaInd, runTab, runLbl, runInd)

    runTab.onMouseClicked = _ => showRun()
    analysisTab.onMouseClicked = _ => showAnalysis()

    val switchBar = new HBox(0):
      prefHeight = 36
      alignment = Pos.CenterLeft
      padding = Insets(0, 0, 0, 8)
      style = s"-fx-background-color: ${Theme.BgBase}; -fx-border-color: ${Theme.BgBorder}; -fx-border-width: 0 0 1 0;"
      children.setAll(runTab, analysisTab)

    showRun()

    new BorderPane:
      style = s"-fx-background-color: ${Theme.BgDeep};"
      top = switchBar
      center = pageArea