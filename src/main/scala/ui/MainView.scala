package ui

import app.AppState
import ui.tabs.{BenchmarkTab, DescriptionTab, LiveStatsTab}
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.*
import scalafx.scene.layout.*
import scalafx.Includes.*

object MainView:
  def apply(): BorderPane =
    val state   = AppState.instance
    val viz     = new VisualizerPanel(state)
    val ctrl    = new ControlPanel(state, viz)
    val metrics = new MetricsPanel(state)

    // ── Header ──────────────────────────────────────────────────
    val dot1 = new javafx.scene.shape.Circle(4):
      setFill(javafx.scene.paint.Color.web(Theme.AccentPrimary))
    val dot2 = new javafx.scene.shape.Circle(4):
      setFill(javafx.scene.paint.Color.web(Theme.AccentSecondary))
    val dot3 = new javafx.scene.shape.Circle(4):
      setFill(javafx.scene.paint.Color.web(Theme.AccentSuccess))

    val dotsBox = new HBox(5)
    dotsBox.alignment = Pos.Center
    dotsBox.padding = Insets(0, 16, 0, 0)
    dotsBox.children.addAll(dot1, dot2, dot3)

    val appTitle = new Label("SORT.VIZ"):
      style = s"-fx-text-fill: ${Theme.TextBright}; -fx-font-size: 16px; " +
        s"-fx-font-weight: bold; -fx-font-family: 'Consolas', monospace;"

    val appSub = new Label("algorithm visualizer"):
      style = s"-fx-text-fill: ${Theme.TextDim}; -fx-font-size: 10px; " +
        s"-fx-font-family: 'Consolas', monospace; -fx-padding: 3 0 0 8;"

    val algoIndicator = new Label(""):
      style = s"-fx-text-fill: ${Theme.AccentPrimary}; -fx-font-size: 10px; " +
        s"-fx-font-family: 'Consolas', monospace;"

    state.selectedAlgorithm.onChange { (_, _, algo) =>
      algoIndicator.text = s"[ ${algo.label.toUpperCase} ]"
    }
    algoIndicator.text = s"[ ${state.selectedAlgorithm.value.label.toUpperCase} ]"

    val spacer = new Region
    HBox.setHgrow(spacer, Priority.Always)

    val versionLbl = new Label("v1.0.0"):
      style = s"-fx-text-fill: ${Theme.TextDim}; -fx-font-size: 9px; " +
        s"-fx-font-family: 'Consolas', monospace; -fx-padding: 0 4 0 0;"

    val header = new HBox
    header.alignment = Pos.CenterLeft
    header.padding = Insets(0, 16, 0, 16)
    header.prefHeight = 44
    header.style = s"-fx-background-color: ${Theme.BgDeep}; " +
      s"-fx-border-color: ${Theme.BgBorder}; -fx-border-width: 0 0 1 0;"
    header.children.addAll(dotsBox, appTitle.delegate, appSub.delegate,
      spacer, algoIndicator.delegate, versionLbl.delegate)

    // ── Canvas region ────────────────────────────────────────────
    val canvasWrapper = new javafx.scene.layout.Region:
      getChildren.add(viz.canvas.delegate)
      override def layoutChildren(): Unit =
        val w = getWidth
        val h = getHeight
        if w > 10 && h > 10 && w < 16384 && h < 16384 then
          viz.canvas.delegate.setWidth(w)
          viz.canvas.delegate.setHeight(h)
          viz.forceRedraw()

    javafx.scene.layout.HBox.setHgrow(canvasWrapper, javafx.scene.layout.Priority.ALWAYS)
    javafx.scene.layout.VBox.setVgrow(canvasWrapper, javafx.scene.layout.Priority.ALWAYS)

    val visualizerRow = new HBox
    visualizerRow.style = s"-fx-background-color: ${Theme.BgDeep};"
    visualizerRow.children.addAll(
      ctrl.panel.delegate,
      canvasWrapper,
      metrics.panel.delegate
    )
    VBox.setVgrow(visualizerRow, Priority.Always)

    // ── Tabs ─────────────────────────────────────────────────────
    val descTab  = new DescriptionTab(() => state.selectedAlgorithm.value)
    val benchTab = new BenchmarkTab(state)
    val statsTab = new LiveStatsTab(state)

    val tabPane = new TabPane
    tabPane.style = Theme.tabPaneStyle
    tabPane.prefHeight = 300
    tabPane.minHeight  = 200

    def makeTab(title: String, node: javafx.scene.Node): Tab =
      val t = new Tab
      t.text     = title
      t.closable = false
      t.content  = node
      t

    tabPane.tabs.addAll(
      makeTab("  📖  DESC  ",   descTab.build().delegate),
      makeTab("  ⚡  BENCH  ",  benchTab.build().delegate),
      makeTab("  📊  STATS  ",  statsTab.build().delegate)
    )

    state.selectedAlgorithm.onChange { (_, _, _) =>
      tabPane.tabs.get(0).setContent(descTab.build().delegate)
    }

    val centerBox = new VBox
    centerBox.style = s"-fx-background-color: ${Theme.BgDeep};"
    centerBox.children.addAll(visualizerRow.delegate, tabPane.delegate)
    VBox.setVgrow(visualizerRow, Priority.Always)

    val bp = new BorderPane
    bp.style  = s"-fx-background-color: ${Theme.BgDeep};"
    bp.top    = header
    bp.center = centerBox
    bp