package ui

import app.AppState
import scalafx.scene.control.Label
import scalafx.scene.layout.*

object MainView:
  def apply(): BorderPane =
    val state   = AppState.instance
    val viz     = new VisualizerPanel(state)
    val ctrl    = new ControlPanel(state, viz)
    val metrics = new MetricsPanel(state)

    val header = new Label("Sorting Algorithm Visualizer"):
      style = """-fx-text-fill: #7986cb;
                |-fx-font-size: 20px;
                |-fx-font-weight: bold;
                |-fx-padding: 12 20 8 20;
                |-fx-background-color: #12142b;""".stripMargin

    val canvasWrapper = new StackPane:
      style = "-fx-background-color: #0f111a;"
      children = viz.canvas
      HBox.setHgrow(this, Priority.Always)

    val mainContent = new HBox(0, ctrl.panel, canvasWrapper, metrics.panel)

    val bp = new BorderPane
    bp.style  = "-fx-background-color: #12142b;"
    bp.top    = header
    bp.center = mainContent
    bp