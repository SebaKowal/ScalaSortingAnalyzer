package ui.visualizer

import app.AppState
import scalafx.scene.layout.*
import ui.utils.Theme
import ui.visualizer.{VisualizerPanel, LeftPanel, RightPanel}

object VisualizerPage:
  def build(state: AppState): HBox =
    val viz   = new VisualizerPanel(state)
    val left  = new LeftPanel(state, viz)
    val right = new RightPanel(state)

    val canvasWrapper = new javafx.scene.layout.Region:
      getChildren.add(viz.root.delegate)
      override def layoutChildren(): Unit =
        val w = getWidth
        val h = getHeight
        if w > 10 && h > 10 && w < 16384 && h < 16384 then
          if viz.isNonComparative then
            viz.topCanvas.delegate.setWidth(w)
            viz.mainCanvas.delegate.setWidth(w)
            viz.topCanvas.delegate.setHeight(h * 0.25)
            viz.mainCanvas.delegate.setHeight(h * 0.75)
          else
            viz.mainCanvas.delegate.setWidth(w)
            viz.mainCanvas.delegate.setHeight(h)
          viz.redraw()

    javafx.scene.layout.HBox.setHgrow(canvasWrapper, javafx.scene.layout.Priority.ALWAYS)
    javafx.scene.layout.VBox.setVgrow(canvasWrapper, javafx.scene.layout.Priority.ALWAYS)

    val row = new HBox
    row.style = s"-fx-background-color: ${Theme.BgDeep};"
    row.children.addAll(
      left.panel.delegate,
      canvasWrapper,
      right.panel.delegate
    )
    VBox.setVgrow(row, Priority.Always)
    row
