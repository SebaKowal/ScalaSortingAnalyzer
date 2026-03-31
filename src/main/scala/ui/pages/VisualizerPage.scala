package ui.pages

import app.AppState
import ui.panel.{LeftPanel, RightPanel}
import ui.{Theme, VisualizerPanel}
import scalafx.scene.layout.*
import scalafx.Includes.*

object VisualizerPage:
  def build(state: AppState): HBox =
    val viz   = new VisualizerPanel(state)
    val left  = new LeftPanel(state, viz)
    val right = new RightPanel(state)

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

    val row = new HBox
    row.style = s"-fx-background-color: ${Theme.BgDeep};"
    row.children.addAll(
      left.panel.delegate,
      canvasWrapper,
      right.panel.delegate
    )
    VBox.setVgrow(row, Priority.Always)
    row